package com.dozuki.ifixit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.user.LoginEvent;
import com.dozuki.ifixit.ui.gallery.GalleryActivity;
import com.dozuki.ifixit.ui.guide.create.GuideCreateActivity;
import com.dozuki.ifixit.ui.guide.create.GuideIntroActivity;
import com.dozuki.ifixit.ui.guide.view.FeaturedGuidesActivity;
import com.dozuki.ifixit.ui.guide.view.LoadingFragment;
import com.dozuki.ifixit.ui.guide.view.TeardownsActivity;
import com.dozuki.ifixit.ui.login.LoginFragment;
import com.dozuki.ifixit.ui.topic_view.TopicActivity;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.ViewServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.otto.Subscribe;
import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Activity that performs various functions that all Activities in this app
 * should do. Such as:
 * <p/>
 * Registering for the event bus. Setting the current site's theme. Finishing
 * the Activity if the user logs out but the Activity requires authentication.
 * Displaying various menu icons.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {
   private static final String STATE_ACTIVE_POSITION = "com.dozuki.ifixit.ui.BaseActivity.activePosition";
   protected static final String LOADING = "LOADING_FRAGMENT";

   private static final int MENU_OVERFLOW = 1;


   /**
    * Slide Out Menu Drawer
    */
   private MenuDrawer mMenuDrawer;

   private MenuAdapter mAdapter;
   private ListView mList;

   private int mActivePosition = -1;

   /**
    * This is incredibly hacky. The issue is that Otto does not search for @Subscribed
    * methods in parent classes because the performance hit is far too big for
    * Android because of the deep inheritance with the framework and views.
    * Because of this
    *
    * @Subscribed methods on BaseActivity itself don't get registered. The
    * workaround is to make an anonymous object that is registered
    * on behalf of the parent class.
    * <p/>
    * Workaround courtesy of:
    * https://github.com/square/otto/issues/26
    * <p/>
    * Note: The '@SuppressWarnings("unused")' is to prevent
    * warnings that are incorrect (the methods *are* actually used.
    */
   private Object loginEventListener = new Object() {
      @SuppressWarnings("unused")
      @Subscribe
      public void onLogin(LoginEvent.Login event) {
         // Reload app to update the menu to include the user name and logout button
         buildSliderMenu();
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onLogout(LoginEvent.Logout event) {
         finishActivityIfPermissionDenied();

         // Reload app to remove username and logout button from menu
         buildSliderMenu();
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onCancel(LoginEvent.Cancel event) {
         finishActivityIfPermissionDenied();
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onUnauthorized(APIEvent.Unauthorized event) {
         LoginFragment.newInstance().show(getSupportFragmentManager(), "LoginFragment");
      }
   };

   public enum Navigation {
      SEARCH, FEATURED_GUIDES, BROWSE_TOPICS, USER_GUIDES, NEW_GUIDE, MEDIA_GALLERY, LOGOUT, USER_FAVORITES,
      YOUTUBE, FACEBOOK, TWITTER, HELP, ABOUT, NOVALUE, TEARDOWNS;

      public static Navigation navigate(String str) {
         try {
            return valueOf(str.toUpperCase());
         } catch (Exception ex) {
            return NOVALUE;
         }
      }
   }

   private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         String tag = (String) view.getTag();
         Context context = parent.getContext();

         if (alertOnNavigation()) {
            navigationAlertDialog(tag, context).show();
         } else {
            EasyTracker.getTracker().sendEvent("menu_action", "drawer_item_click", ((String) view.getTag()).toLowerCase(),
             null);

            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);

            navigateMenuDrawer(tag, context);
         }
      }
   };

   public void navigateMenuDrawer(String tag, Context context) {
      Intent intent;
      String url;

      switch (Navigation.navigate(tag)) {
         case SEARCH:
         case BROWSE_TOPICS:
            intent = new Intent(context, TopicActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case FEATURED_GUIDES:
            intent = new Intent(context, FeaturedGuidesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case TEARDOWNS:
            intent = new Intent(context, TeardownsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case USER_FAVORITES:
            intent = new Intent(context, FavoritesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case USER_GUIDES:
            intent = new Intent(context, GuideCreateActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case NEW_GUIDE:
            intent = new Intent(context, GuideIntroActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;

         case MEDIA_GALLERY:
            intent = new Intent(context, GalleryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            break;
         case LOGOUT:
            MainApplication.get().logout(BaseActivity.this);
            break;

         case YOUTUBE:
            intent = new Intent(Intent.ACTION_VIEW);
            url = "https://www.youtube.com/user/iFixitYourself";

            intent.setData(Uri.parse(url));
            startActivity(intent);
            break;

         case FACEBOOK:
            intent = new Intent(Intent.ACTION_VIEW);
            url = "https://www.facebook.com/iFixit";

            intent.setData(Uri.parse(url));
            startActivity(intent);
            break;

         case TWITTER:
            intent = new Intent(Intent.ACTION_VIEW);
            url = "https://twitter.com/iFixit";

            intent.setData(Uri.parse(url));
            startActivity(intent);
            break;

         case HELP:
         case ABOUT:
      }
      mMenuDrawer.closeMenu();
   }

   @Override
   public void onCreate(Bundle savedState) {
      /**
       * Set the current site's theme. Must be before onCreate because of
       * inflating views.
       */

      setTheme(MainApplication.get().getSiteTheme());
      setTitle("");

      super.onCreate(savedState);

      EasyTracker.getInstance().setContext(this);

      /**
       * There is another register call in onResume but we also need it here for the onUnauthorized
       * call that is usually triggered in onCreate of derived Activities.
       */
      MainApplication.getBus().register(this);
      MainApplication.getBus().register(loginEventListener);

      if (savedState != null) {
         mActivePosition = savedState.getInt(STATE_ACTIVE_POSITION);
      }

      mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.BEHIND, Position.LEFT, MenuDrawer.MENU_DRAG_WINDOW);

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      mMenuDrawer.setMenuSize(getResources().getDimensionPixelSize(R.dimen.menu_size));
      mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);
      mMenuDrawer.setTouchBezelSize(getResources().getDimensionPixelSize(R.dimen.menu_bezel_size));

      buildSliderMenu();

      if (MainApplication.inDebug()) {
         ViewServer.get(this).addWindow(this);
      }
   }

   private void buildSliderMenu() {
      boolean onIfixit = MainApplication.get().getSite().mName.equals("ifixit");

      // Add items to the menu.  The order Items are added is the order they appear in the menu.
      List<Object> items = new ArrayList<Object>();

      //items.add(new Item(getString(R.string.slide_menu_search), R.drawable.ic_action_search, "search"));

      items.add(new Category(getString(R.string.slide_menu_browse_content)));
      items.add(new Item(getString(R.string.slide_menu_browse_devices), R.drawable.ic_action_list_2, "browse_topics"));
      items.add(new Item(getString(R.string.featured_guides), R.drawable.ic_action_star_10, "featured_guides"));
      if (onIfixit) items.add(new Item(getString(R.string.teardowns), R.drawable.ic_menu_stack, "teardowns"));

      items.add(new Category(buildAccountMenuCategoryTitle()));
      items.add(new Item(getString(R.string.slide_menu_favorite_guides), R.drawable.ic_menu_favorite_light, "user_favorites"));
      items.add(new Item(getString(R.string.slide_menu_my_guides), R.drawable.ic_menu_spinner_guides, "user_guides"));
      items.add(new Item(getString(R.string.slide_menu_create_new_guide), R.drawable.ic_menu_add_guide, "new_guide"));
      items.add(new Item(getString(R.string.slide_menu_media_gallery), R.drawable.ic_menu_spinner_gallery, "media_gallery"));

      if (MainApplication.get().isUserLoggedIn()) {
         items.add(new Item(getString(R.string.slide_menu_logout), R.drawable.ic_action_exit, "logout"));
      }

      if (onIfixit) {
         items.add(new Category(getString(R.string.slide_menu_ifixit_everywhere)));
         items.add(new Item(getString(R.string.slide_menu_youtube), R.drawable.ic_action_youtube, "youtube"));
         items.add(new Item(getString(R.string.slide_menu_facebook), R.drawable.ic_action_facebook, "facebook"));
         items.add(new Item(getString(R.string.slide_menu_twitter), R.drawable.ic_action_twitter, "twitter"));
      }

      /*items.add(new Category(getString(R.string.slide_menu_more_info)));
      items.add(new Item(getString(R.string.slide_menu_help), R.drawable.ic_action_help, "help"));
      items.add(new Item(getString(R.string.slide_menu_about), R.drawable.ic_action_info, "about")); */

      // A custom ListView is needed so the drawer can be notified when it's scrolled. This is to update the position
      // of the arrow indicator.
      mList = new ListView(this);
      mAdapter = new MenuAdapter(items);
      mList.setAdapter(mAdapter);
      mList.setOnItemClickListener(mItemClickListener);
      mList.setCacheColorHint(Color.TRANSPARENT);

      mMenuDrawer.setMenuView(mList);
      mMenuDrawer.setSlideDrawable(R.drawable.ic_drawer);
      mMenuDrawer.setDrawerIndicatorEnabled(true);

      mMenuDrawer.invalidate();
   }

   /**
    * Close the menu drawer if back is pressed and the menu is open.
    */
   @Override
   public void onBackPressed() {
      final int drawerState = mMenuDrawer.getDrawerState();
      if (drawerState == MenuDrawer.STATE_OPEN
       || drawerState == MenuDrawer.STATE_OPENING) {
         mMenuDrawer.closeMenu();
         return;
      }

      super.onBackPressed();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
   }

   /**
    * If the user is coming back to this Activity make sure they still have
    * permission to view it. onRestoreInstanceState is for Activities that are
    * being recreated and onRestart is for Activities who are merely being
    * restarted. Unfortunately both are needed.
    */
   @Override
   public void onRestoreInstanceState(Bundle savedState) {
      super.onRestoreInstanceState(savedState);
      finishActivityIfPermissionDenied();
   }

   @Override
   public void onStart() {
      super.onStart();

      this.overridePendingTransition(0, 0);

      // Start analytics tracking
      EasyTracker.getInstance().activityStart(this);
   }

   @Override
   public void onStop() {
      super.onStop();

      // Stop analytics tracking
      EasyTracker.getInstance().activityStop(this);
   }


   @Override
   public void onRestart() {
      super.onRestart();
      finishActivityIfPermissionDenied();

      // Invalidate the options menu in case the user logged in/out in a child Activity.
      buildSliderMenu();
   }

   @Override
   public void onResume() {
      super.onResume();

      MainApplication.getBus().register(this);
      MainApplication.getBus().register(loginEventListener);

      if (MainApplication.inDebug())
         ViewServer.get(this).setFocusedWindow(this);
   }

   @Override
   public void onDestroy() {
      super.onDestroy();

      if (MainApplication.inDebug())
         ViewServer.get(this).removeWindow(this);
   }

   @Override
   public void onPause() {
      super.onPause();

      MainApplication.getBus().unregister(this);
      MainApplication.getBus().unregister(loginEventListener);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case android.R.id.home:
            mMenuDrawer.toggleMenu();
            return true;
      }

      return super.onOptionsItemSelected(item);
   }

   private static class Item {
      String mTitle;
      int mIconRes;
      String mTag;

      Item(String title, int iconRes, String tag) {
         mTitle = title;
         mIconRes = iconRes;
         mTag = tag;
      }
   }

   private static class Category {
      String mTitle;

      Category(String title) {
         mTitle = title;
      }
   }

   private class MenuAdapter extends BaseAdapter {
      private List<Object> mItems;
      private static final int VIEW_TYPE_COUNT = 2;

      MenuAdapter(List<Object> items) {
         mItems = items;
      }

      @Override
      public int getCount() {
         return mItems.size();
      }

      @Override
      public Object getItem(int position) {
         return mItems.get(position);
      }

      @Override
      public long getItemId(int position) {
         return position;
      }

      @Override
      public int getItemViewType(int position) {
         return getItem(position) instanceof Item ? 0 : 1;
      }

      @Override
      public int getViewTypeCount() {
         return VIEW_TYPE_COUNT;
      }

      @Override
      public boolean isEnabled(int position) {
         return getItem(position) instanceof Item;
      }

      @Override
      public boolean areAllItemsEnabled() {
         return false;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;
         Object item = getItem(position);

         if (item instanceof Category) {
            if (v == null) {
               v = getLayoutInflater().inflate(R.layout.menu_row_category, parent, false);
            }

            ((TextView) v).setText(((Category) item).mTitle);

         } else {
            if (v == null) {
               v = getLayoutInflater().inflate(R.layout.menu_row_item, parent, false);
            }

            TextView tv = (TextView) v;
            tv.setText(((Item) item).mTitle);
            tv.setCompoundDrawablesWithIntrinsicBounds(((Item) item).mIconRes, 0, 0, 0);
            tv.setTag(((Item) item).mTag);
         }

         v.setTag(R.id.mdActiveViewPosition, position);

         if (position == mActivePosition) {
            mMenuDrawer.setActiveView(v, position);
         }

         return v;
      }
   }

   /**
    * Whether the activity show warn the user before navigating away using the MenuDrawer.
    * @return
    */
   public boolean alertOnNavigation() {
      return false;
   }

   public AlertDialog navigationAlertDialog(String tag, Context context) {
      return null;
   }

   /**
    * Finishes the Activity if the user should be logged in but isn't.
    */
   private void finishActivityIfPermissionDenied() {
      MainApplication app = MainApplication.get();

      /**
       * Never finish if user is logged in or is logging in.
       */
      if (app.isUserLoggedIn() || app.isLoggingIn()) {
         return;
      }

      /**
       * Finish if the site is private or activity requires authentication.
       */
      if (!neverFinishActivityOnLogout()
       && (finishActivityIfLoggedOut() || !app.getSite().mPublic)) {
         finish();
      }
   }

   /**
    * "Settings" methods for derived classes are found below. Decides when to
    * finish the Activity, what icons to display etc.
    */

   /**
    * Returns true if the Activity should be finished if the user logs out or
    * cancels authentication.
    */
   public boolean finishActivityIfLoggedOut() {
      return false;
   }

   /**
    * Returns true if the Activity should never be finished despite meeting
    * other conditions.
    * <p/>
    * This exists because of a race condition of sorts involving logging out of
    * private Dozuki sites. SiteListActivity can't reset the current site to
    * one that is public so it is erroneously finished unless flagged
    * otherwise.
    */
   public boolean neverFinishActivityOnLogout() {
      return false;
   }

   private String buildAccountMenuCategoryTitle() {
      MainApplication app = MainApplication.get();
      boolean loggedIn = app.isUserLoggedIn();
      String title;

      if (loggedIn) {
         String username = app.getUser().getUsername();
         title = getString(R.string.account_username_title, username);
      } else {
         title = getString(R.string.account_menu_title);
      }

      return title;
   }

   public void showLoading(int container) {
      showLoading(container, getString(R.string.loading));
   }

   public void showLoading(int container, String message) {
      getSupportFragmentManager().beginTransaction()
       .add(container, new LoadingFragment(message), LOADING).addToBackStack(LOADING)
       .commit();
   }

   public void hideLoading() {
      getSupportFragmentManager().popBackStack(LOADING, FragmentManager.POP_BACK_STACK_INCLUSIVE);
   }
}
