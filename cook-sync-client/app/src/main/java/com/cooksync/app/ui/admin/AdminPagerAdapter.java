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

    public AdminPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @Override
    public int getItemCount() {
        return 4;
    }

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
