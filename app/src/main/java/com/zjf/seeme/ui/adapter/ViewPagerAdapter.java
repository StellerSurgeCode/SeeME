package com.zjf.seeme.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.zjf.seeme.ui.fragment.CategoriesFragment;
import com.zjf.seeme.ui.fragment.OverviewFragment;
import com.zjf.seeme.ui.fragment.TodoFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return OverviewFragment.newInstance();
            case 1: return CategoriesFragment.newInstance();
            case 2: return TodoFragment.newInstance();
            default: return OverviewFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
