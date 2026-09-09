package com.cooksync.app.ui.recipe.importing;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.base.BaseActivity;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.ui.recipe.wizard.AddRecipeWizardActivity;
import com.cooksync.app.util.CloudinaryUploader;
import com.cooksync.app.util.LocalImageCache;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry screen for smart recipe import: paste a web-page URL, or pick one or more photos
 * (camera-captured cookbook pages, a printed recipe, handwritten notes) — more than one photo
 * lets a recipe that spans several pages/cards be read as one. Either path starts an async
 * server-side job, shown here as a simple progress state while it's polled, and — once it
 * succeeds — hands the resulting (private) recipe straight to the existing edit wizard
 * ({@link AddRecipeWizardActivity#startEdit}) for the human review every import must pass
 * through before it can ever be published.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 05/09/2026
 */
public class RecipeImportActivity extends BaseActivity {

    private RecipeImportViewModel viewModel;

    private View groupForm;
    private View groupProgress;
    private EditText etUrl;
    private TextView tvStatus;
    private View btnStartPhotoImport;
    private RecipeImportPhotoAdapter photoAdapter;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    /** Local (already app-owned) URIs of every photo picked so far, in reading order. */
    private final List<Uri> pendingPhotoUris = new ArrayList<>();
    /** Secure Cloudinary URLs collected as {@link #pendingPhotoUris} are uploaded one at a time. */
    private final List<String> uploadedPhotoUrls = new ArrayList<>();
    /** Index into {@link #pendingPhotoUris} of the photo currently being uploaded. */
    private int uploadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_import);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(RecipeImportViewModel.class);

        groupForm = findViewById(R.id.group_import_form);
        groupProgress = findViewById(R.id.group_import_progress);
        etUrl = findViewById(R.id.et_recipe_url);
        tvStatus = findViewById(R.id.tv_import_status);
        btnStartPhotoImport = findViewById(R.id.btn_start_photo_import);

        RecyclerView rvPhotos = findViewById(R.id.rv_import_photos);
        photoAdapter = new RecipeImportPhotoAdapter();
        photoAdapter.setOnRemoveListener(this::removePhoto);
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvPhotos.setAdapter(photoAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        findViewById(R.id.btn_import_web).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (url.isEmpty()) {
                showError(getString(R.string.recipe_import_url_required), etUrl);
                return;
            }
            showProgress(getString(R.string.recipe_import_status_pending));
            viewModel.startWebImport(url);
        });

        findViewById(R.id.btn_import_photo).setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        btnStartPhotoImport.setOnClickListener(v -> startPhotoUploadSequence());

        findViewById(R.id.btn_cancel_import).setOnClickListener(v -> {
            viewModel.cancelPolling();
            showForm();
        });

        setupObservers();
    }

    private void onPhotoPicked(Uri pickedUri) {
        if (pickedUri == null) {
            return;
        }
        LocalImageCache.copyToPrivateCache(this, pickedUri, "recipe_import_", localUri -> {
            if (localUri == null) {
                showError(getString(R.string.recipe_import_photo_failed), findViewById(R.id.btn_import_photo));
                return;
            }
            pendingPhotoUris.add(localUri);
            photoAdapter.setPhotos(pendingPhotoUris);
            btnStartPhotoImport.setVisibility(View.VISIBLE);
        });
    }

    private void removePhoto(int position) {
        if (position < 0 || position >= pendingPhotoUris.size()) {
            return;
        }
        pendingPhotoUris.remove(position);
        photoAdapter.setPhotos(pendingPhotoUris);
        btnStartPhotoImport.setVisibility(pendingPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Kicks off the photo import: uploads every picked photo to Cloudinary one at a time (see
     * {@link #uploadNextPhoto}), then starts the server-side job with the resulting URLs once
     * all uploads succeed.
     */
    private void startPhotoUploadSequence() {
        uploadedPhotoUrls.clear();
        uploadIndex = 0;
        showProgress(uploadingStatusLabel());
        uploadNextPhoto();
    }

    /**
     * Requests an upload signature for the next not-yet-uploaded photo, or — once every photo has
     * been uploaded — starts the import job with the collected URLs.
     */
    private void uploadNextPhoto() {
        if (uploadIndex >= pendingPhotoUris.size()) {
            showProgress(getString(R.string.recipe_import_status_pending));
            viewModel.startPhotoImport(new ArrayList<>(uploadedPhotoUrls));
            return;
        }
        tvStatus.setText(uploadingStatusLabel());
        viewModel.requestPhotoUploadSignature();
    }

    private String uploadingStatusLabel() {
        return pendingPhotoUris.size() <= 1
                ? getString(R.string.recipe_import_status_uploading)
                : getString(R.string.recipe_import_uploading_photo_format, uploadIndex + 1, pendingPhotoUris.size());
    }

    private void setupObservers() {
        viewModel.getSignatureResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<CloudinarySignatureResponse> success
                    && uploadIndex < pendingPhotoUris.size()) {
                Uri uploadingUri = pendingPhotoUris.get(uploadIndex);
                CloudinaryUploader.upload(this, uploadingUri, viewModel.getPendingFolder(), viewModel.getPendingPublicId(),
                        success.getData(), new CloudinaryUploader.Callback() {
                            @Override
                            public void onSuccess(@NonNull String secureUrl) {
                                uploadedPhotoUrls.add(secureUrl);
                                uploadIndex++;
                                uploadNextPhoto();
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                showError(message, null);
                                showForm();
                            }
                        });
            } else if (result instanceof ApiResult.Error<?> error) {
                showError(error.getMessage(), null);
                showForm();
            }
        });

        viewModel.getJobStatus().observe(this, this::renderJobStatus);

        viewModel.getRecipeReadyEvent().observe(this, event -> {
            com.dtos.response.recipe.RecipeResponse recipe = event.getContentIfNotHandled();
            if (recipe != null) {
                AddRecipeWizardActivity.startEdit(this, recipe);
                finish();
            }
        });

        viewModel.getErrorEvent().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                OrganicToast.showError(this, null, message);
                showForm();
            }
        });
    }

    private void renderJobStatus(RecipeImportJobResponse job) {
        if (job == null) {
            return;
        }
        String label = switch (job.status()) {
            case "PROCESSING" -> getString(R.string.recipe_import_status_processing);
            default -> getString(R.string.recipe_import_status_pending);
        };
        tvStatus.setText(label);
    }

    private void showProgress(String status) {
        tvStatus.setText(status);
        groupForm.setVisibility(View.GONE);
        groupProgress.setVisibility(View.VISIBLE);
    }

    private void showForm() {
        groupProgress.setVisibility(View.GONE);
        groupForm.setVisibility(View.VISIBLE);
    }
}
