package com.example.recetario.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.recetario.fragments.MyRecipesFragment
import com.example.recetario.fragments.SavedRecipesFragment

class ProfileSectionsPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SavedRecipesFragment()
            else -> MyRecipesFragment()
        }
    }
}
