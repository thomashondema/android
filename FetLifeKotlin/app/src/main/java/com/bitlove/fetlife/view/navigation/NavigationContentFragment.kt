package com.bitlove.fetlife.view.navigation

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitlove.fetlife.FetLifeApplication
import com.bitlove.fetlife.R
import com.bitlove.fetlife.view.widget.SlideControlViewPager
import com.bitlove.fetlife.view.widget.SlidingTabLayout

class NavigationContentFragment : Fragment() {

    companion object {
        private const val ARG_KEY_NAVIGATION = "ARG_KEY_NAVIGATION"
        private const val ARG_KEY_LAYOUT = "ARG_KEY_LAYOUT"

        fun newFragment(navigation: Int, layout: NavigationCallback.Layout? = null) : NavigationContentFragment {
            val args = Bundle()
            args.putInt(ARG_KEY_NAVIGATION,navigation)
            args.putSerializable(ARG_KEY_LAYOUT,layout)
            val contentFragment = NavigationContentFragment()
            contentFragment.arguments = args
            return contentFragment
        }
    }

    val navigationFragmentFactory = FetLifeApplication.instance.navigationFragmentFactory

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater!!.inflate(R.layout.fragment_navigation_content,container,false)
        val navigation = arguments.getInt(ARG_KEY_NAVIGATION)
        val layout = arguments.getSerializable(ARG_KEY_LAYOUT) as? NavigationCallback.Layout

        val adapter = navigationFragmentFactory.createNavigationFragmentAdapter(fragmentManager,navigation,layout)

        val viewPager = view.findViewById<SlideControlViewPager>(R.id.content_view_pager)
        viewPager.adapter = adapter
        val tabs = view.findViewById<SlidingTabLayout>(R.id.navigation_tabs)
        //TODO move to xml
        tabs.setDividerColorResource(R.color.silver)
        tabs.setSelectedIndicatorColorResource(R.color.bostonUniversityRed)
        if (adapter.count < 2) {
            tabs.visibility = View.GONE
        } else {
            tabs.setViewPager(viewPager)
        }

        return view
    }

}