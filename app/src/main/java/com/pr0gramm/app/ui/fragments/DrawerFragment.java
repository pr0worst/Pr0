package com.pr0gramm.app.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.pr0gramm.app.GraphDrawable;
import com.pr0gramm.app.R;
import com.pr0gramm.app.Settings;
import com.pr0gramm.app.WrapContentLinearLayoutManager;
import com.pr0gramm.app.api.pr0gramm.Info;
import com.pr0gramm.app.feed.FeedFilter;
import com.pr0gramm.app.feed.FeedType;
import com.pr0gramm.app.orm.Bookmark;
import com.pr0gramm.app.services.BookmarkService;
import com.pr0gramm.app.services.UserService;
import com.pr0gramm.app.ui.SettingsActivity;
import com.pr0gramm.app.ui.dialogs.ErrorDialogFragment;
import com.pr0gramm.app.ui.dialogs.LoginDialogFragment;
import com.pr0gramm.app.ui.dialogs.LogoutDialogFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Actions;

import static com.google.common.base.Objects.equal;
import static java.util.Arrays.asList;
import static rx.android.observables.AndroidObservable.bindFragment;

/**
 */
public class DrawerFragment extends RoboFragment {
    @Inject
    private UserService userService;

    @Inject
    private Settings settings;

    @Inject
    private BookmarkService bookmarkService;

    @InjectView(R.id.username)
    private TextView usernameView;

    @InjectView(R.id.benis)
    private TextView benisView;

    @InjectView(R.id.benis_container)
    private View benisContainer;

    @InjectView(R.id.benis_graph)
    private ImageView benisGraph;

    @InjectView(R.id.action_login)
    private View loginView;

    @InjectView(R.id.action_logout)
    private View logoutView;

    @InjectView(R.id.action_settings)
    private View settingsView;

    @InjectView(R.id.drawer_nav_list)
    private RecyclerView navItemsRecyclerView;

    @InjectResource(R.drawable.ic_black_action_favorite)
    private Drawable iconFavorites;

    @InjectResource(R.drawable.ic_black_action_home)
    private Drawable iconFeedTypePromoted;

    @InjectResource(R.drawable.ic_black_action_line_chart)
    private Drawable iconFeedTypeNew;

    @InjectResource(R.drawable.ic_black_action_pin)
    private Drawable iconBookmark;

    private final NavigationAdapter navigationAdapter = new NavigationAdapter();

    private ColorStateList defaultColor = ColorStateList.valueOf(Color.BLACK);
    private ColorStateList markedColor;
    private Subscription scLoginState;
    private Subscription scNavigationItems;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(getActivity(), R.style.Theme_AppCompat_Light);
        inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.left_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get "marked" color
        int primary = getActivity().getResources().getColor(R.color.primary);
        markedColor = ColorStateList.valueOf(primary);

        // initialize the top navigation items
        navItemsRecyclerView.setAdapter(navigationAdapter);
        navItemsRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(
                getActivity(), WrapContentLinearLayoutManager.VERTICAL, false));

        // add the static items to the navigation
        navigationAdapter.setNavigationItems(getFixedNavigationItems(Optional.<Info.User>absent()));

        settingsView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        loginView.setOnClickListener(v -> {
            LoginDialogFragment dialog = new LoginDialogFragment();
            dialog.show(getFragmentManager(), null);
        });

        logoutView.setOnClickListener(v -> {
            LogoutDialogFragment fragment = new LogoutDialogFragment();
            fragment.show(getFragmentManager(), null);
        });

        benisGraph.setOnClickListener(this::onBenisGraphClicked);
    }

    /**
     * Adds the default "fixed" items to the menu
     */
    private List<NavigationItem> getFixedNavigationItems(Optional<Info.User> userInfo) {
        List<NavigationItem> items = new ArrayList<>();
        items.add(new NavigationItem(
                new FeedFilter().withFeedType(FeedType.PROMOTED),
                getString(R.string.action_feed_type_promoted),
                iconFeedTypePromoted));

        items.add(new NavigationItem(
                new FeedFilter().withFeedType(FeedType.NEW),
                getString(R.string.action_feed_type_new),
                iconFeedTypeNew));

        if (userInfo.isPresent()) {
            String name = userInfo.get().getName();
            items.add(new NavigationItem(
                    new FeedFilter().withFeedType(FeedType.NEW).withLikes(name),
                    getString(R.string.action_favorites),
                    iconFavorites));
        }

        return items;
    }

    private void onBenisGraphClicked(View view) {
        new MaterialDialog.Builder(getActivity())
                .content(R.string.benis_graph_explanation)
                .positiveText(R.string.okay)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        scLoginState = bindFragment(this, userService.getLoginStateObservable())
                .subscribe(this::onLoginStateChanged, Actions.empty());

        scNavigationItems = bindFragment(this, newNavigationItemsObservable())
                .subscribe(navigationAdapter::setNavigationItems, ErrorDialogFragment.defaultOnError());

        benisGraph.setVisibility(settings.benisGraphEnabled() ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("unchecked")
    private Observable<List<NavigationItem>> newNavigationItemsObservable() {
        // observe and merge the menu items from different sources
        return Observable.combineLatest(asList(
                userService.getLoginStateObservable().map(st -> st.getInfo() != null
                        ? getFixedNavigationItems(Optional.of(st.getInfo().getUser()))
                        : getFixedNavigationItems(Optional.<Info.User>absent())),

                bookmarkService.get().map(this::bookmarksToNavItem)), args -> {

            ArrayList<NavigationItem> result = new ArrayList<>();
            for (Object arg : args)
                result.addAll((List<NavigationItem>) arg);

            return result;
        });
    }

    private List<NavigationItem> bookmarksToNavItem(List<Bookmark> entries) {
        return Lists.transform(entries, entry -> {
            Drawable icon = iconBookmark.getConstantState().newDrawable();
            String title = entry.getTitle().toUpperCase();
            return new NavigationItem(entry.asFeedFilter(), title, icon, entry);
        });
    }

    @Override
    public void onPause() {
        if (scLoginState != null)
            scLoginState.unsubscribe();

        if (scNavigationItems != null)
            scNavigationItems.unsubscribe();

        super.onPause();
    }

    /**
     * Fakes the drawable tint by applying a color filter on all compound
     * drawables of this view.
     *
     * @param view  The view to "tint"
     * @param color The color with which the drawables are to be tinted.
     */
    private static void changeCompoundDrawableColor(TextView view, ColorStateList color) {
        int defaultColor = color.getDefaultColor();
        Drawable[] drawables = view.getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable == null)
                continue;

            // fake the tint with a color filter.
            drawable.mutate().setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void onLoginStateChanged(UserService.LoginState state) {
        if (state.isAuthorized()) {
            Info.User user = state.getInfo().getUser();
            usernameView.setText(user.getName());

            String benisValue = String.valueOf(user.getScore());

            benisView.setText(benisValue);
            benisContainer.setVisibility(View.VISIBLE);
            benisGraph.setImageDrawable(new GraphDrawable(state.getBenisHistory()));

            loginView.setVisibility(View.GONE);
            logoutView.setVisibility(View.VISIBLE);
        } else {
            usernameView.setText(R.string.pr0gramm);
            benisContainer.setVisibility(View.GONE);
            benisGraph.setImageDrawable(null);

            loginView.setVisibility(View.VISIBLE);
            logoutView.setVisibility(View.GONE);
        }
    }

    public void updateCurrentFilters(FeedFilter current) {
        navigationAdapter.setCurrentFilter(current);
    }

    public interface OnFeedFilterSelected {
        /**
         * Called if a drawer filter was clicked.
         *
         * @param filter The feed filter that was clicked.
         */
        void onFeedFilterSelectedInNavigation(FeedFilter filter);
    }

    private void onFeedFilterClicked(FeedFilter filter) {
        if (getActivity() instanceof OnFeedFilterSelected) {
            ((OnFeedFilterSelected) getActivity()).onFeedFilterSelectedInNavigation(filter);
        }
    }

    private class NavigationAdapter extends RecyclerView.Adapter<NavigationItemViewHolder> {
        private final List<NavigationItem> allItems = new ArrayList<>();
        private Optional<NavigationItem> selected = Optional.absent();
        private FeedFilter currentFilter;

        @Override
        public NavigationItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.left_drawer_nav_item, parent, false);

            return new NavigationItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NavigationItemViewHolder holder, int position) {
            NavigationItem item = allItems.get(position);
            holder.text.setText(item.title);

            // set the icon of the image
            holder.text.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);

            // update color
            ColorStateList color = (selected.orNull() == item) ? markedColor : defaultColor;
            holder.text.setTextColor(color);
            changeCompoundDrawableColor(holder.text, color);

            // handle clicks
            holder.itemView.setOnClickListener(v -> {
                onFeedFilterClicked(item.filter);
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (item.bookmark != null) {
                    showDialogToRemoveBookmark(item.bookmark);
                }

                return true;
            });
        }

        @Override
        public int getItemCount() {
            return allItems.size();
        }

        public void setNavigationItems(List<NavigationItem> items) {
            this.allItems.clear();
            this.allItems.addAll(items);
            merge();
        }

        public void setCurrentFilter(FeedFilter current) {
            currentFilter = current;
            merge();
        }

        private void merge() {
            // calculate the currently selected item
            selected = FluentIterable.from(allItems)
                    .filter(nav -> equal(currentFilter, nav.filter))
                    .first();

            if (!selected.isPresent() && currentFilter != null) {
                selected = FluentIterable.from(allItems)
                        .filter(nav -> nav.filter.getFeedType() == currentFilter.getFeedType())
                        .first();
            }

            notifyDataSetChanged();
        }
    }

    private void showDialogToRemoveBookmark(Bookmark bookmark) {
        new MaterialDialog.Builder(getActivity())
                .content(R.string.do_you_want_to_remove_this_bookmark)
                .positiveText(R.string.yes)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        bookmarkService.delete(bookmark);
                    }
                })
                .show();
    }

    private class NavigationItemViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        public NavigationItemViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView;
        }
    }

    private class NavigationItem {
        final String title;
        final FeedFilter filter;
        final Drawable icon;
        final Bookmark bookmark;

        NavigationItem(FeedFilter filter, String title, Drawable icon) {
            this(filter, title, icon, null);
        }

        NavigationItem(FeedFilter filter, String title, Drawable icon, Bookmark bookmark) {
            this.title = title;
            this.filter = filter;
            this.icon = icon;
            this.bookmark = bookmark;
        }
    }
}
