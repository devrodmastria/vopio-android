package info.vopio.android.OnboardingViews

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import info.vopio.android.R
import info.vopio.android.databinding.ActivityOnboardingBinding


class OnboardingActivity : AppCompatActivity() {

    lateinit var thisSectionsPagerAdapter: SectionsPagerAdapter
    lateinit var nextBtn: Button
    lateinit var signinBtn: Button

    lateinit var imageZero: ImageView
    lateinit var imageOne: ImageView
    lateinit var imageTwo: ImageView
    lateinit var indicators: Array<ImageView>
    lateinit var thisViewPager: ViewPager

    private var page = 0
    val SLIDER_DURATION : Long = 7000

    private lateinit var binding: ActivityOnboardingBinding

    lateinit var swipeHandler: Handler

    private val autoSwipeTask = object : Runnable{
        override fun run() {

            // start at page 0
            thisViewPager.setCurrentItem(page, true)

            // update to next page for next swipe - zero-index
            if (page < 2) { page += 1 } else { page = 0 }

            swipeHandler.postDelayed(this, SLIDER_DURATION)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        thisSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        signinBtn = binding.introBtnSignIn
        nextBtn = binding.introBtnNext

        imageZero = binding.introIndicator0
        imageOne = binding.introIndicator1
        imageTwo = binding.introIndicator2
        indicators = arrayOf(imageZero, imageOne, imageTwo)

        thisViewPager = binding.container
        thisViewPager.adapter = thisSectionsPagerAdapter
        thisViewPager.currentItem = page
        updateIndicators(page)

        val color1 = ContextCompat.getColor(this, R.color.blue_500)
        val color2 = ContextCompat.getColor(this, R.color.green_500)
        val color3 = ContextCompat.getColor(this, R.color.orange_500)

        val colorList = intArrayOf(color1, color2, color3)
        val evaluator = ArgbEvaluator()

        // setup action bar color to match first viewpager fragment
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    applicationContext,
                    android.R.color.holo_blue_dark
                )
            )
        )

        swipeHandler = Handler(Looper.getMainLooper())

        thisViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

                /*
                color update
                 */
                val colorUpdate = evaluator.evaluate(
                    positionOffset, colorList[position],
                    colorList[if (position == 2) position else position + 1]
                ) as Int
                thisViewPager.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                page = position
                updateIndicators(page)
                when (position) {
                    0 -> {
                        thisViewPager.setBackgroundColor(color1)

                        // make sure to change this in the onCreate section whenever it's changed here.
                        supportActionBar?.setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    applicationContext,
                                    android.R.color.holo_blue_dark
                                )
                            )
                        )
                    }
                    1 -> {
                        thisViewPager.setBackgroundColor(color2)
                        supportActionBar?.setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    applicationContext,
                                    android.R.color.holo_green_dark
                                )
                            )
                        )
                    }
                    2 -> {
                        thisViewPager.setBackgroundColor(color3)
                        supportActionBar?.setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    applicationContext,
                                    android.R.color.holo_orange_dark
                                )
                            )
                        )
                    }
                }
                nextBtn.visibility = if (position == 2) View.GONE else View.VISIBLE
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        nextBtn.setOnClickListener {
            page += 1
            thisViewPager.setCurrentItem(page, true)
        }

        signinBtn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

    }

    override fun onPause() {
        super.onPause()
        // stop auto swipe
        swipeHandler.removeCallbacks(autoSwipeTask)
    }

    override fun onResume() {
        super.onResume()
        // begin auto swipe
        swipeHandler.post(autoSwipeTask)
    }

    private fun updateIndicators(position: Int) {
        for (i in indicators.indices) {
            indicators.get(i).setBackgroundResource(
                if (i == position) R.drawable.indicator_selected else R.drawable.indicator_unselected
            )
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */

    class PlaceholderFragment: Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            val rootView: View = inflater.inflate(R.layout.fragment_on_boarding, container, false)
            val textView = rootView.findViewById<View>(R.id.section_label) as TextView
            val textTitle = rootView.findViewById<View>(R.id.section_title) as TextView
            val imageView = rootView.findViewById<View>(R.id.imageViewOnBoard) as AppCompatImageView

            val typeface = context?.let { ResourcesCompat.getFont(it, R.font.titillium_regular) }

            textView.setTypeface(typeface, Typeface.NORMAL)
            textTitle.setTypeface(typeface, Typeface.NORMAL)
            val res = resources
            val section = requireArguments().getInt(ARG_SECTION_NUMBER)
            when (section) {
                1 -> {
                    // Icon made by https://www.flaticon.com/authors/smashicons
                    // https://www.flaticon.com/authors/eucalyp
                    textView.text = getString(R.string.onboard_one)
                    textTitle.text = getString(R.string.onboard_one_title)
                    imageView.setBackgroundResource(R.drawable.ic_instructor)
                }
                2 -> {
                    // Icon made by https://creativemarket.com/eucalyp
                    textView.text = res.getString(R.string.onboard_two)
                    textTitle.text = getString(R.string.onboard_two_title)
                    imageView.setBackgroundResource(R.drawable.ic_support)
                }
                3 -> {
                    // Icon made by https://www.flaticon.com/authors/smashicons
                    // icon made by www.freepik.com
                    textView.text = getString(R.string.onboard_three)
                    textTitle.text = getString(R.string.onboard_three_title)
                    imageView.setBackgroundResource(R.drawable.ic_search_engine)
                }

                else -> {
                    textView.text = getString(R.string.onboard_three)
                    Log.d("Log", "Default onboard view " + ARG_SECTION_NUMBER)
                }
            }

            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    // todo update Fragment Pager
    class SectionsPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(
                position + 1
            )
        }

        override fun getCount(): Int {
            // Show total number of pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "SECTION 1"
                1 -> return "SECTION 2"
                2 -> return "SECTION 3"
                3 -> return "SECTION 4"
            }
            return null
        }
    }

}