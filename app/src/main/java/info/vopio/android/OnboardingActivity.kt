package info.vopio.android

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import info.vopio.android.databinding.ActivityOnboardingBinding


class OnboardingActivity : AppCompatActivity() {

    lateinit var thisSectionsPagerAdapter: SectionsPagerAdapter
    lateinit var nextBtn: Button
    lateinit var skipBtn: Button
    lateinit var finishBtn: Button

    lateinit var zero: ImageView
    lateinit var one: ImageView
    lateinit var two: ImageView
    lateinit var three: ImageView
    lateinit var indicators: Array<ImageView>
    lateinit var thisViewPager: ViewPager

    lateinit var thisFirebaseAnalytics: FirebaseAnalytics
    lateinit var thisFirebaseAuth: FirebaseAuth

    private var page = 0

    private lateinit var binding: ActivityOnboardingBinding

    lateinit var swipeHandler: Handler

    private val autoSwipeTask = object : Runnable{
        override fun run() {

            // start at page 0
            thisViewPager.setCurrentItem(page, true)

            // update to next page for next swipe - zero-index
            if (page < 4) { page += 1 } else { page = 0 }

            swipeHandler.postDelayed(this, 7000)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        thisFirebaseAnalytics = FirebaseAnalytics.getInstance(applicationContext)

        thisSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        skipBtn = binding.introBtnSignIn
        finishBtn = binding.introBtnFinish
        nextBtn = binding.introBtnNext

        zero = binding.introIndicator0
        one = binding.introIndicator1
        two = binding.introIndicator2
        three = binding.introIndicator3
        indicators = arrayOf(zero, one, two, three)

        thisViewPager = binding.container
        thisViewPager.adapter = thisSectionsPagerAdapter
        thisViewPager.currentItem = page
        updateIndicators(page)

        val color1 = ContextCompat.getColor(this, R.color.page_one)
        val color2 = ContextCompat.getColor(this, R.color.page_two)
        val color3 = ContextCompat.getColor(this, R.color.page_three)
        val color4 = ContextCompat.getColor(this, R.color.page_two)

        val colorList = intArrayOf(color1, color2, color3, color4)
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

        val appTitle: String = getString(R.string.app_name)
        //attempted to set custom font on action bar - ignored by Android - requires its own view
        val typeface = this.let { ResourcesCompat.getFont(this, R.font.titillium_regular) }
        val spanString = SpannableString(appTitle)
        spanString.setSpan(typeface?.style, 0, appTitle.length-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.title = spanString

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
                    colorList[if (position == 3) position else position + 1]
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
                    3 -> {
                        thisViewPager.setBackgroundColor(color4)
                        supportActionBar?.setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    applicationContext,
                                    android.R.color.holo_green_dark
                                )
                            )
                        )
                    }
                }
                nextBtn.visibility = if (position == 3) View.GONE else View.VISIBLE
//                finishBtn.visibility = if (position == 3) View.VISIBLE else View.GONE
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        nextBtn.setOnClickListener {
            page += 1
            thisViewPager.setCurrentItem(page, true)
        }

        skipBtn.setOnClickListener {
            val eventMessage = "Skipped_onboarding"
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "skipped_onboarding")
            thisFirebaseAnalytics.logEvent(eventMessage, bundle)

            trySignIn()
        }

        finishBtn.setOnClickListener {
            val eventMessage = "Finished_onboarding"
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "finished_onboard")
            thisFirebaseAnalytics.logEvent(eventMessage, bundle)

            trySignIn()

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

    private fun trySignIn(){

        // Initialize Firebase Auth
        thisFirebaseAuth = FirebaseAuth.getInstance()
        val thisFirebaseUser = thisFirebaseAuth.currentUser
        if (thisFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

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
                4 -> {
                    // Icon made by https://creativemarket.com/eucalyp
                    textView.text = res.getString(R.string.onboard_four)
                    textTitle.text = getString(R.string.onboard_four_title)
                    imageView.setBackgroundResource(R.drawable.ic_languages)
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

    class SectionsPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(
                position + 1
            )
        }

        override fun getCount(): Int {
            // Show number total pages.
            return 4
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