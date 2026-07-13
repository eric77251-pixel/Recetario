package com.example.recetario.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
// Importamos tu nuevo fragmento de borradores
import com.example.recetario.fragments.DraftRecipesFragment
import com.example.recetario.fragments.MyRecipesFragment
import com.example.recetario.fragments.SavedRecipesFragment

class ProfileSectionsPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    // Cambiamos a 3 para reflejar las tres pestañas (Guardadas, Mis Recetas, Borradores)
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SavedRecipesFragment()
            1 -> MyRecipesFragment()
            2 -> DraftRecipesFragment()
            else -> SavedRecipesFragment()
        }
    }
}