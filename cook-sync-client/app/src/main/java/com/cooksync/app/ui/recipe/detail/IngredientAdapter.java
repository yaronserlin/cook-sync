package com.cooksync.app.ui.recipe.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.data.model.recipe.IngredientQuantityFormatter;
import com.cooksync.app.data.model.recipe.IngredientScaler;
import com.cooksync.app.data.model.recipe.UnitDisplayFormatter;
import com.cooksync.app.ui.base.BaseAdapter;
import com.dtos.response.ingredient.IngredientResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adapter for the recipe ingredients list. Displays each ingredient's quantity scaled to
 * whatever serving count the recipe detail screen's stepper currently has selected (see
 * {@link #setServings}), rather than the recipe's original quantities.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public class IngredientAdapter extends BaseAdapter<IngredientResponse, IngredientAdapter.ViewHolder> {

    /** The recipe's original serving count; quantities are scaled from this to {@link #targetServings}. */
    private int originalServings = 1;
    /** The currently-selected serving count to scale displayed quantities to. */
    private int targetServings = 1;

    /**
     * Replaces the displayed ingredient list.
     *
     * @param newIngredients the complete ingredient list to display
     */
    public void setIngredients(List<IngredientResponse> newIngredients) {
        setItems(newIngredients);
    }

    /**
     * Sets the serving counts used to scale every displayed quantity, and re-renders the list.
     *
     * @param originalServings the recipe's own serving count, as authored
     * @param targetServings the serving count currently selected on the stepper
     */
    public void setServings(int originalServings, int targetServings) {
        this.originalServings = originalServings;
        this.targetServings = targetServings;
        notifyDataSetChanged();
    }

    /**
     * Inflates a new ingredient row view holder.
     *
     * @param parent the RecyclerView this row is being added to
     * @param viewType the view type, unused (single row layout)
     * @return the inflated view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds an ingredient's name and quantity+unit amount to its row view holder.
     *
     * @param holder the row view holder to bind
     * @param position the ingredient's position in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngredientResponse ingredient = getItem(position);
        holder.name.setText(ingredient.name());

        BigDecimal scaledQuantity = IngredientScaler.scale(ingredient.quantity(), originalServings, targetServings);
        String unitName = UnitDisplayFormatter.displayName(ingredient.unit(), scaledQuantity);
        String amount = IngredientQuantityFormatter.format(scaledQuantity) + " " + unitName;
        holder.amount.setText(amount.trim());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView amount;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.ingredient_name);
            amount = view.findViewById(R.id.ingredient_amount);
        }
    }
}
