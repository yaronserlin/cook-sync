package com.cooksync.app.ui.admin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Supplies {@link AdminConsoleActivity}'s four moderation tabs — Reports, Tags, Users, Units —
 * to its {@link androidx.viewpager2.widget.ViewPager2}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class AdminPagerAdapter extends FragmentStateAdapter {

    /** ViewPager2 position of the Reports tab. */
    public static final int TAB_REPORTS = 0;
    /** ViewPager2 position of the Tags tab. */
    public static final int TAB_TAGS = 1;
    /** ViewPager2 position of the Users tab. */
    public static final int TAB_USERS = 2;
    /** ViewPager2 position of the Units tab. */
    public static final int TAB_UNITS = 3;

    /**
     * Constructs the pager adapter bound to its host activity.
     *
     * @param activity the hosting {@link AdminConsoleActivity}
     */
    public AdminPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    /**
     * Returns the fixed number of tabs.
     *
     * @return the number of tabs, always 4
     */
    @Override
    public int getItemCount() {
        return 4;
    }

    /**
     * Builds the fragment for a given tab position.
     *
     * @param position one of the {@code TAB_*} constants
     * @return the fragment backing that tab
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case TAB_REPORTS -> new AdminReportsFragment();
            case TAB_TAGS -> new AdminTagsFragment();
            case TAB_USERS -> new AdminUsersFragment();
            default -> new AdminUnitsFragment();
        };
    }
}
