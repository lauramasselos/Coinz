package com.example.s1603459.myapplication

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_navigation.*

class NavigationActivity : AppCompatActivity(), ProfileFragment.OnFragmentInteractionListener, MapFragment.OnFragmentInteractionListener, BankFragment.OnFragmentInteractionListener {

    override fun onFragmentInteraction(uri: Uri) {

    }


    private val mapFragment: Fragment = MapFragment()
    private val bankFragment: Fragment = BankFragment()
    private val profileFragment: Fragment = ProfileFragment()

    private lateinit var currentFragment : Fragment

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                changeFragment(profileFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                changeFragment(mapFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                changeFragment(bankFragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun changeFragment(fragment: Fragment){
        val t = supportFragmentManager.beginTransaction()
        t.hide(currentFragment)
        t.show(fragment)
        t.commit()
        currentFragment = fragment
    }

    private fun addFragments() {
        val t = supportFragmentManager.beginTransaction()
        t.add(R.id.container, mapFragment, mapFragment.tag)
        t.add(R.id.container, bankFragment, bankFragment.tag)
        t.add(R.id.container, profileFragment, profileFragment.tag)
        t.hide(profileFragment)
        t.hide(bankFragment)
        t.show(mapFragment)
        t.commit()
        currentFragment = mapFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        addFragments()
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapFragment.onDestroy()
        profileFragment.onDestroy()
        bankFragment.onDestroy()
    }
}
