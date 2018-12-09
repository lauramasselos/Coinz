package com.example.s1603459.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map-> {
                startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_bank -> {
                return@OnNavigationItemSelectedListener true
            }

        }
        false
    }
    Menu menu = navigation.getMenu();
    MenuItem menuItem = menu.getItem(INSERT_INDEX_HERE);
    menuItem.setChecked(true);

}
