package com.cooksync.app.data.model.recipe;

import com.dtos.response.recipe.DescriptionBlockDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class for collecting and resolving pending Cloudinary image uploads within a {@link RecipeDraft}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/08/2026
 */
public final class RecipeDraftMediaHelper {

    /** One picked-but-not-yet-uploaded image item in a draft. */
    public static final class PendingImageUpload {
        /** Which slot in the draft this pending image belongs to. */
        public enum Kind { COVER, DESCRIPTION_BLOCK, INSTRUCTION }

        private final Kind kind;
        private final String localUri;
        private final DescriptionBlockDTO descriptionBlock;
        private final RecipeDraft.DraftInstruction instruction;

        /**
         * @param kind which slot in the draft this pending image belongs to
         * @param localUri the not-yet-uploaded local ({@code content://} or {@code file://}) URI
         * @param descriptionBlock the owning description block, if {@code kind} is {@code DESCRIPTION_BLOCK}; otherwise {@code null}
         * @param instruction the owning instruction step, if {@code kind} is {@code INSTRUCTION}; otherwise {@code null}
         */
        public PendingImageUpload(Kind kind, String localUri, DescriptionBlockDTO descriptionBlock,
                                  RecipeDraft.DraftInstruction instruction) {
            this.kind = kind;
            this.localUri = localUri;
            this.descriptionBlock = descriptionBlock;
            this.instruction = instruction;
        }

        /** @return the not-yet-uploaded local ({@code content://} or {@code file://}) URI */
        public String getLocalUri() {
            return localUri;
        }

        /** @return which slot in the draft this pending image belongs to */
        public Kind getKind() {
            return kind;
        }

        /** @return the owning description block, if {@link #getKind()} is {@code DESCRIPTION_BLOCK}; otherwise {@code null} */
        public DescriptionBlockDTO getDescriptionBlock() {
            return descriptionBlock;
        }

        /** @return the owning instruction step, if {@link #getKind()} is {@code INSTRUCTION}; otherwise {@code null} */
        public RecipeDraft.DraftInstruction getInstruction() {
            return instruction;
        }
    }

    private RecipeDraftMediaHelper() {
        // Utility
    }

    /**
     * Collects every local (file:// or content://) image reference in the draft that needs upload.
     *
     * @param draft target recipe draft
     * @return list of pending image upload descriptors
     */
    public static List<PendingImageUpload> collectPendingImageUploads(RecipeDraft draft) {
        List<PendingImageUpload> pending = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (draft.descriptionBlocks != null) {
            for (DescriptionBlockDTO block : draft.descriptionBlocks) {
                if ("IMAGE".equals(block.type()) && isLocalUri(block.imageUrl())) {
                    pending.add(new PendingImageUpload(PendingImageUpload.Kind.DESCRIPTION_BLOCK, block.imageUrl(), block, null));
                    seen.add(block.imageUrl());
                }
            }
        }
        if (isLocalUri(draft.primaryImageUrl) && !seen.contains(draft.primaryImageUrl)) {
            pending.add(new PendingImageUpload(PendingImageUpload.Kind.COVER, draft.primaryImageUrl, null, null));
        }
        if (draft.instructions != null) {
            for (RecipeDraft.DraftInstruction instruction : draft.instructions) {
                if (isLocalUri(instruction.imageUrl)) {
                    pending.add(new PendingImageUpload(PendingImageUpload.Kind.INSTRUCTION, instruction.imageUrl, null, instruction));
                }
            }
        }
        return pending;
    }

    /**
     * Replaces a local image URI in the draft with its resulting secure HTTPS Cloudinary URL.
     *
     * @param draft target recipe draft
     * @param pending pending image descriptor
     * @param uploadedUrl secure Cloudinary HTTPS URL
     */
    public static void resolvePendingImageUpload(RecipeDraft draft, PendingImageUpload pending, String uploadedUrl) {
        if (Objects.equals(draft.primaryImageUrl, pending.localUri)) {
            draft.primaryImageUrl = uploadedUrl;
        }
        switch (pending.kind) {
            case DESCRIPTION_BLOCK -> {
                if (draft.descriptionBlocks != null) {
                    int index = draft.descriptionBlocks.indexOf(pending.descriptionBlock);
                    if (index >= 0) {
                        draft.descriptionBlocks.set(index, new DescriptionBlockDTO(
                                pending.descriptionBlock.type(), pending.descriptionBlock.text(), uploadedUrl, pending.descriptionBlock.caption()));
                    }
                }
            }
            case INSTRUCTION -> {
                if (pending.instruction != null) {
                    pending.instruction.imageUrl = uploadedUrl;
                }
            }
            case COVER -> { /* already handled above */ }
        }
    }

    /**
     * Clears a not-yet-uploaded (or failed-to-upload) local image reference from the draft,
     * leaving the corresponding slot empty rather than pointing at a dead local URI. Used as a
     * publish fallback when Cloudinary upload fails partway through: images already resolved via
     * {@link #resolvePendingImageUpload} are left untouched, only the remaining pending ones are cleared.
     *
     * @param draft target recipe draft
     * @param pending pending image descriptor to clear
     */
    public static void clearPendingImage(RecipeDraft draft, PendingImageUpload pending) {
        switch (pending.kind) {
            case COVER -> {
                if (Objects.equals(draft.primaryImageUrl, pending.localUri)) {
                    draft.primaryImageUrl = null;
                }
            }
            case DESCRIPTION_BLOCK -> {
                if (draft.descriptionBlocks != null) {
                    draft.descriptionBlocks.remove(pending.descriptionBlock);
                }
            }
            case INSTRUCTION -> {
                if (pending.instruction != null) {
                    pending.instruction.imageUrl = null;
                }
            }
        }
    }

    /**
     * Reports whether {@code value} is a local URI (picked but not yet uploaded), as opposed to
     * an already-uploaded {@code https://} Cloudinary URL.
     *
     * @param value the URI string to check, may be {@code null}
     * @return {@code true} if {@code value} starts with {@code content://} or {@code file://}
     */
    private static boolean isLocalUri(String value) {
        return value != null && (value.startsWith("content://") || value.startsWith("file://"));
    }
}
