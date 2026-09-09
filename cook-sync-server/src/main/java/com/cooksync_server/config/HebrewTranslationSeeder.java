package com.cooksync_server.config;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.repositories.ContentTranslationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds the human-reviewed Hebrew translations for shared catalog data. Entity
 * IDs are taken from the current seed run, so regenerated UUIDs never orphan
 * translations.
 */
@Component
@RequiredArgsConstructor
class HebrewTranslationSeeder {

    private static final String HEBREW = "he";

    private static final Map<String, String> TAG_TRANSLATIONS = Map.ofEntries(
            Map.entry("vegan", "טבעוני"),
            Map.entry("quick", "מהיר"),
            Map.entry("healthy", "בריא"),
            Map.entry("breakfast", "ארוחת בוקר"),
            Map.entry("dinner", "ארוחת ערב"),
            Map.entry("dessert", "קינוח"),
            Map.entry("gluten-free", "ללא גלוטן"),
            Map.entry("gluten free", "ללא גלוטן"),
            Map.entry("high-protein", "עתיר חלבון"),
            Map.entry("high protein", "עתיר חלבון"),
            Map.entry("comfort-food", "אוכל מנחם"),
            Map.entry("comfort food", "אוכל מנחם"),
            Map.entry("spicy", "חריף"),
            Map.entry("vegetarian", "צמחוני"),
            Map.entry("italian", "איטלקי"),
            Map.entry("asian", "אסייתי"),
            Map.entry("mexican", "מקסיקני"),
            Map.entry("middle-eastern", "מזרח תיכוני"),
            Map.entry("french", "צרפתי"),
            Map.entry("soup", "מרק"),
            Map.entry("salad", "סלט"),
            Map.entry("baking", "אפייה"),
            Map.entry("seafood", "פירות ים")
    );

    private static final Map<String, String> UNIT_TRANSLATIONS = Map.ofEntries(
            Map.entry("cup", "כוס"),
            Map.entry("tbsp", "כף"),
            Map.entry("tsp", "כפית"),
            Map.entry("g", "גרם"),
            Map.entry("kg", "קילוגרם"),
            Map.entry("ml", "מיליליטר"),
            Map.entry("l", "ליטר"),
            Map.entry("pinch", "קורט"),
            Map.entry("clove", "שן"),
            Map.entry("piece", "יחידה"),
            Map.entry("slice", "פרוסה"),
            Map.entry("can", "פחית"),
            Map.entry("pkg", "חבילה"),
            Map.entry("handful", "חופן"),
            Map.entry("sprig", "ענף"),
            Map.entry("bundle", "צרור")
    );

    private static final Map<String, String> UNIT_PLURAL_TRANSLATIONS = Map.ofEntries(
            Map.entry("cup", "כוסות"),
            Map.entry("tbsp", "כפות"),
            Map.entry("tsp", "כפיות"),
            Map.entry("g", "גרמים"),
            Map.entry("kg", "קילוגרמים"),
            Map.entry("ml", "מיליליטרים"),
            Map.entry("l", "ליטרים"),
            Map.entry("pinch", "קורטים"),
            Map.entry("clove", "שיניים"),
            Map.entry("piece", "יחידות"),
            Map.entry("slice", "פרוסות"),
            Map.entry("can", "פחיות"),
            Map.entry("pkg", "חבילות"),
            Map.entry("handful", "חופנים"),
            Map.entry("sprig", "ענפים"),
            Map.entry("bundle", "צרורות")
    );

    private static final Map<String, String> RECIPE_TITLE_TRANSLATIONS = Map.ofEntries(
            Map.entry("Classic Spaghetti Carbonara", "ספגטי קרבונרה קלאסי"),
            Map.entry("Authentic Middle Eastern Shakshuka", "שקשוקה מזרח תיכונית אותנטית"),
            Map.entry("Japanese Chicken Teriyaki Bowl", "קערת עוף טריאקי יפנית"),
            Map.entry("Authentic Mexican Beef Birria Tacos", "טאקוס בירייה בקר מקסיקני אותנטי"),
            Map.entry("Creamy Tuscan Garlic Chicken", "עוף בשום קרמי בסגנון טוסקני"),
            Map.entry("Fresh Greek Salad with Feta & Olives", "סלט יווני טרי עם פטה וזיתים"),
            Map.entry("Japanese Matcha Soufflé Pancakes", "פנקייק סופלה מאצ'ה יפני"),
            Map.entry("Classic French Onion Soup", "מרק בצל צרפתי קלאסי"),
            Map.entry("Gourmet Avocado Toast with Poached Egg", "טוסט אבוקדו גורמה עם ביצה פושה"),
            Map.entry("Crispy Lemon Garlic Roasted Salmon", "סלמון צלוי פריך בלימון ושום"),
            Map.entry("Authentic Thai Green Chicken Curry", "קארי עוף ירוק תאילנדי אותנטי"),
            Map.entry("Classic French Beef Bourguignon", "בורגיניון בקר צרפתי קלאסי"),
            Map.entry("Berry Acai Smoothie Bowl", "קערת סמוזי אסאי ופירות יער"),
            Map.entry("Gourmet Double Cheeseburger with Secret Sauce", "המבורגר גורמה כפול עם רוטב סודי"),
            Map.entry("Creamy Wild Mushroom Risotto", "ריזוטו פטריות בר קרמי"),
            Map.entry("Spanish Seafood Paella", "פאייה ספרדית עם פירות ים"),
            Map.entry("Decadent Chocolate Molten Lava Cake", "עוגת שוקולד לבה נוזלית עשירה"),
            Map.entry("Crispy Falafel Pita Pocket with Tahini", "כיס פיתה עם פלאפל פריך וטחינה"),
            Map.entry("Traditional Vietnamese Beef Pho", "פו וייטנאמי מסורתי עם בקר"),
            Map.entry("Classic Chicken Caesar Salad", "סלט קיסר קלאסי עם עוף"),
            Map.entry("Authentic Indian Butter Chicken", "עוף בחמאה הודי אותנטי"),
            Map.entry("Mediterranean Grilled Chicken Souvlaki", "סובלאקי עוף על האש ים-תיכוני"),
            Map.entry("Authentic Italian Margherita Pizza", "פיצה מרגריטה איטלקית אותנטית"),
            Map.entry("Crispy Tofu Buddha Bowl with Peanut Dressing", "קערת בודהה עם טופו פריך ורוטב בוטנים"),
            Map.entry("Savoyard Potato Tartiflette", "טרטיפלט תפוחי אדמה סבויארי"),
            Map.entry("Homemade New York Style Cheesecake", "עוגת גבינה ביתית בסגנון ניו יורק"),
            Map.entry("Authentic Mexican Huevos Rancheros", "הוארוס ראנצ'רוס מקסיקני אותנטי"),
            Map.entry("Creamy Tomato Soup & Crispy Grilled Cheese", "מרק עגבניות קרמי וטוסט גבינה פריך"),
            Map.entry("Spicy Seared Ahi Tuna Poke Bowl", "פוקי בול חריף עם טונה אהי צרובה"),
            Map.entry("Cinnamon Roll French Toast Bake", "מאפה טוסט צרפתי בטעם רול קינמון")
    );

    private final ContentTranslationRepository translationRepository;

    @Transactional
    void seedCatalogTranslations(List<Tag> tags, List<Unit> units) {
        tags.forEach(tag -> seed(ContentTranslation.EntityType.TAG_NAME, tag.getId(),
                TAG_TRANSLATIONS.get(tag.getName().toLowerCase())));
        units.forEach(unit -> {
            String code = unit.getCode().toLowerCase();
            seed(ContentTranslation.EntityType.UNIT_NAME, unit.getId(), UNIT_TRANSLATIONS.get(code));
            seed(ContentTranslation.EntityType.UNIT_NAME_PLURAL, unit.getId(), UNIT_PLURAL_TRANSLATIONS.get(code));
        });
    }

    @Transactional
    void seedRecipeTitleTranslations(List<Recipe> recipes) {
        recipes.forEach(recipe -> seed(ContentTranslation.EntityType.RECIPE_TITLE, recipe.getId(),
                RECIPE_TITLE_TRANSLATIONS.get(recipe.getTitle())));
    }

    private void seed(ContentTranslation.EntityType type, String entityId, String value) {
        if (value == null) {
            return;
        }
        translationRepository.insertIfAbsent(
                java.util.UUID.randomUUID().toString(), type.name(), entityId, HEBREW, value,
                ContentTranslation.Source.HUMAN.name(), java.time.LocalDateTime.now());
    }
}
