package com.haroldadmin.moonshot.launchDetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.carousel
import com.haroldadmin.moonshot.R as appR
import com.haroldadmin.moonshot.MainViewModel
import com.haroldadmin.moonshot.base.MoonShotFragment
import com.haroldadmin.moonshot.base.asyncTypedEpoxyController
import com.haroldadmin.moonshot.base.withModelsFrom
import com.haroldadmin.moonshot.core.Resource
import com.haroldadmin.moonshot.itemError
import com.haroldadmin.moonshot.itemExpandableTextWithHeading
import com.haroldadmin.moonshot.itemLaunchCard
import com.haroldadmin.moonshot.itemLaunchDetail
import com.haroldadmin.moonshot.itemLoading
import com.haroldadmin.moonshot.itemTextHeader
import com.haroldadmin.moonshot.itemTextWithHeading
import com.haroldadmin.moonshot.launchDetails.databinding.FragmentLaunchDetailsBinding
import com.haroldadmin.moonshot.models.launch.LaunchMinimal
import com.haroldadmin.moonshot.models.launch.LaunchStats
import com.haroldadmin.moonshot.utils.format
import com.haroldadmin.vector.withState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class LaunchDetailsFragment : MoonShotFragment() {

    private lateinit var binding: FragmentLaunchDetailsBinding
    private val safeArgs by navArgs<LaunchDetailsFragmentArgs>()
    private val viewModel by viewModel<LaunchDetailsViewModel> {
        parametersOf(LaunchDetailsState(safeArgs.flightNumber), safeArgs.flightNumber)
    }
    private val mainViewModel by sharedViewModel<MainViewModel>()
    private val differ by inject<Handler>(named("differ"))
    private val builder by inject<Handler>(named("builder"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LaunchDetails.init()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLaunchDetailsBinding.inflate(inflater, container, false)
        val animation = AnimationUtils.loadLayoutAnimation(requireContext(), appR.anim.layout_animation_fade_in)
        binding.rvLaunchDetails.apply {
            setController(epoxyController)
            layoutAnimation = animation
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner, Observer { renderState() })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        epoxyController.cancelPendingModelBuild()
    }

    override fun renderState() = withState(viewModel) { state ->
        epoxyController.setData(state)
        if (state.launch is Resource.Success && state.launch.data.missionName != null) {
            mainViewModel.setTitle(state.launch.data.missionName!!)
        }
    }

    private val epoxyController by lazy {
        asyncTypedEpoxyController(builder, differ, viewModel) { state ->

            when (val launch = state.launch) {
                is Resource.Success -> {
                    buildLaunchModels(this, launch.data)
                }

                is Resource.Error<LaunchMinimal, *> -> {
                    itemError {
                        id("launch-error")
                        error(getString(R.string.fragmentLaunchDetailsErrorMessage))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }

                    if (launch.data != null) {
                        buildLaunchModels(this, launch.data!!)
                    }
                }
                else -> itemLoading {
                    id("launch-loading")
                    message(getString(R.string.fragmentLaunchDetailsLoadingMessage))
                    spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                }
            }

            when (val stats = state.launchStats) {
                is Resource.Success -> {
                    itemTextHeader {
                        id("rocket")
                        header(getString(R.string.fragmentLaunchDetailsRocketSummaryHeader))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }
                    if (stats.data.rocket != null) {
                        buildLaunchStats(stats.data, this)
                    }
                }
                is Resource.Error<LaunchStats, *> -> {
                    itemTextHeader {
                        id("rocket")
                        header(getString(R.string.fragmentLaunchDetailsRocketSummaryHeader))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }
                    itemError {
                        id("rocket-summary-error")
                        error(getString(R.string.fragmentLaunchDetailsRocketSummaryError))
                        spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                    }
                    if (stats.data != null) {
                        buildLaunchStats(stats.data!!, this)
                    }
                }
                else -> Unit
            }

            when (val pictures = state.launchPictures) {
                is Resource.Success -> {
                    if (pictures.data.images.isNotEmpty()) {
                        itemTextHeader {
                            id("photos")
                            header("Photos")
                            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                        }
                        carousel {
                            id("launch-pictures")
                            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
                            withModelsFrom(pictures.data.images) { url ->
                                ItemLaunchPictureBindingModel_()
                                    .id(url)
                                    .imageUrl(url)
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun buildLaunchModels(controller: EpoxyController, launch: LaunchMinimal) {
        with(controller) {
            itemLaunchCard {
                id("header")
                launch(launch)
                spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            }
            itemLaunchDetail {
                id("launch-date")
                detailHeader(getString(R.string.fragmentLaunchDetailsLaunchDateHeader))
                detailName(
                    launch.launchDate?.format(resources.configuration)
                        ?: getString(R.string.fragmentLaunchDetailsNoLaunchDateMessage)
                )
                detailIcon(ContextCompat.getDrawable(requireContext(), appR.drawable.ic_round_date_range_24px))
                spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            }
            itemLaunchDetail {
                id("launch-site")
                detailHeader(getString(R.string.fragmentLaunchDetailsLaunchSiteHeader))
                detailName(launch.siteName ?: getString(R.string.fragmentLaunchDetailsNoLaunchSiteMessage))
                detailIcon(ContextCompat.getDrawable(requireContext(), appR.drawable.ic_round_place_24px))
                onDetailClick { _ -> showLaunchPadDetails(launch.siteId!!) }
                spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            }
            itemLaunchDetail {
                id("launch-success")
                detailHeader(getString(R.string.fragmentLaunchDetailsLaunchStatusHeader))
                detailName(launch.launchSuccessText)
                detailIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_flight_takeoff_24px))
                spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            }
            itemExpandableTextWithHeading {
                id("launch-details")
                heading(getString(R.string.fragmentLaunchDetailsLaunchDetailsHeader))
                text(launch.details ?: getString(R.string.fragmentLaunchDetailsNoLaunchDetailsMessage))
                spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            }

            launch
                .links
                .filterValues { !it.isNullOrBlank() }
                .takeIf { it.isNotEmpty() }
                ?.let { map ->
                    buildLinks(map as Map<String, String>, this)
                }
        }
    }

    private fun buildLaunchStats(stats: LaunchStats, controller: EpoxyController) = with(controller) {
        itemLaunchRocket {
            id("rocket-summary")
            rocketSummary(stats.rocket)
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            onRocketClick { _ ->
                LaunchDetailsFragmentDirections.launchRocketDetails(
                    stats.rocket!!.rocketId
                ).also { action ->
                    findNavController().navigate(action)
                }
            }
        }
        itemTextWithHeading {
            id("first-stage-summary")
            heading(getString(R.string.fragmentLaunchDetailsFirstStageSummaryHeader))
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / 2 }
            text("Cores: ${stats.firstStageCoreCounts}")
        }
        itemTextWithHeading {
            id("second-stage-summary")
            heading(getString(R.string.fragmentLaunchDetailsSecondStageSummaryHeader))
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount / 2 }
            text("Payloads: ${stats.secondStagePayloadCounts}")
        }
    }

    private fun buildLinks(links: Map<String, String>, controller: EpoxyController) = with(controller) {
        itemTextHeader {
            id("links")
            header(getString(R.string.fragmentLaunchDetailsLinksHeader))
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
        }

        carousel {
            id("launch-links")
            spanSizeOverride { totalSpanCount, _, _ -> totalSpanCount }
            withModelsFrom(links) { name, link ->
                when {
                    name.contains("YouTube") -> {
                        ItemYoutubeLinkBindingModel_()
                            .id(link)
                            .thumbnailUrl(link.youtubeThumbnail())
                            .onYoutubeClick { _ ->
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(link.youtubeVideo())
                                ).also { startActivity(it) }
                            }
                    }
                    name.contains("Reddit") -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_reddit)
                        ItemInternetLinkBindingModel_()
                            .id(link)
                            .title(name)
                            .backgroundGradient(drawable)
                            .onLinkClick { _ ->
                                Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    .also { startActivity(it) }
                            }
                    }
                    else -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_wikipedia)
                        ItemInternetLinkBindingModel_()
                            .id(link)
                            .title(name)
                            .backgroundGradient(drawable)
                            .onLinkClick { _ ->
                                Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    .also { startActivity(it) }
                            }
                    }
                }
            }
        }
    }

    private fun showLaunchPadDetails(siteId: String) {
        LaunchDetailsFragmentDirections.launchPadDetails(siteId)
            .let { action ->
                findNavController().navigate(action)
            }
    }
}