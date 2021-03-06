package com.dozuki.ifixit.ui.topic_view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.topic.TopicNode;
import com.dozuki.ifixit.model.topic.TopicSelectedListener;
import com.dozuki.ifixit.ui.BaseActivity;
import com.dozuki.ifixit.ui.guide.view.LoadingFragment;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.APIService;
import com.squareup.otto.Subscribe;

public class TopicActivity extends BaseActivity
 implements TopicSelectedListener, OnBackStackChangedListener {
   private static final String ROOT_TOPIC = "ROOT_TOPIC";
   private static final String TOPIC_LIST_VISIBLE = "TOPIC_LIST_VISIBLE";
   protected static final long TOPIC_LIST_HIDE_DELAY = 1;

   /**
    * Used for Dozuki app. Enables the up navigation button to finish the
    * activity and go back to the sites list.
    */
   private static final boolean UP_NAVIGATION_FINISH_ACTIVITY = false;

   private TopicViewFragment mTopicView;
   private FrameLayout mTopicViewOverlay;
   private TopicNode mRootTopic;
   private int mBackStackSize = 0;
   private boolean mDualPane;
   private boolean mHideTopicList;
   private boolean mTopicListVisible;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.topics);

      mTopicView = (TopicViewFragment) getSupportFragmentManager()
       .findFragmentById(R.id.topic_view_fragment);
      mTopicViewOverlay = (FrameLayout) findViewById(R.id.topic_view_overlay);
      mHideTopicList = mTopicViewOverlay != null;
      mDualPane = mTopicView != null && mTopicView.isInLayout();

      if (savedInstanceState != null) {
         mRootTopic = (TopicNode) savedInstanceState.getSerializable(ROOT_TOPIC);
         mTopicListVisible = savedInstanceState.getBoolean(TOPIC_LIST_VISIBLE);
      } else {
         mTopicListVisible = true;
      }

      if (mRootTopic == null) {
         showLoading(R.id.topic_list_fragment);
         APIService.call(this, APIService.getCategoriesAPICall());
      }

      if (!mTopicListVisible && !mHideTopicList) {
         getSupportFragmentManager().popBackStack();
      }

      if (mTopicListVisible && mHideTopicList && mTopicView.isDisplayingTopic()) {
         hideTopicListWithDelay();
      }

      getSupportFragmentManager().addOnBackStackChangedListener(this);

      // Reset backstack size
      mBackStackSize = -1;
      onBackStackChanged();

      if (mTopicViewOverlay != null) {
         mTopicViewOverlay.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
               if (mTopicListVisible && mTopicView.isDisplayingTopic()) {
                  hideTopicList();
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }

   @Subscribe
   public void onCategories(APIEvent.Categories event) {
      hideLoading();
      if (!event.hasError()) {
         if (mRootTopic == null) {
            mRootTopic = event.getResult();
            onTopicSelected(mRootTopic);
         }
      } else {
         APIService.getErrorDialog(this, event).show();
      }
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      outState.putSerializable(ROOT_TOPIC, mRootTopic);
      outState.putBoolean(TOPIC_LIST_VISIBLE, mTopicListVisible);
   }

   @Override
   public void onBackStackChanged() {
      int backStackSize = getSupportFragmentManager().getBackStackEntryCount();

      if (mBackStackSize > backStackSize) {
         setTopicListVisible();
      }

      mBackStackSize = backStackSize;
   }

   @Override
   public void onTopicSelected(TopicNode topic) {
      if (topic.isLeaf()) {
         if (mDualPane) {
            mTopicView.setTopicNode(topic);

            if (mHideTopicList) {
               hideTopicList();
            }
         } else {
            Intent intent = new Intent(this, TopicViewActivity.class);
            Bundle bundle = new Bundle();

            bundle.putSerializable(TopicViewActivity.TOPIC_KEY, topic);
            intent.putExtras(bundle);
            startActivity(intent);
         }
      } else {
         if (mDualPane && !topic.isRoot()) {
            mTopicView.setTopicNode(topic);
         }
         changeTopicListView(new TopicListFragment(topic), !topic.isRoot());
      }
   }

   protected boolean isDualPane() {
      return mDualPane;
   }

   private void hideTopicList() {
      hideTopicList(false);
   }

   private void hideTopicList(boolean delay) {
      mTopicViewOverlay.setVisibility(View.INVISIBLE);
      mTopicListVisible = false;
      changeTopicListView(new Fragment(), true, delay);
   }

   private void hideTopicListWithDelay() {
      // Delay this slightly to make sure the animation is played.
      new Handler().postAtTime(new Runnable() {
         public void run() {
            hideTopicList(true);
         }
      }, SystemClock.uptimeMillis() + TOPIC_LIST_HIDE_DELAY);
   }

   private void setTopicListVisible() {
      if (mTopicViewOverlay != null) {
         mTopicViewOverlay.setVisibility(View.VISIBLE);
      }
      mTopicListVisible = true;
   }

   private void changeTopicListView(Fragment fragment, boolean addToBack) {
      changeTopicListView(fragment, addToBack, false);
   }

   private void changeTopicListView(Fragment fragment, boolean addToBack,
    boolean delay) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      int inAnim, outAnim;

      if (delay) {
         inAnim = R.anim.slide_in_right_delay;
         outAnim = R.anim.slide_out_left_delay;
      } else {
         inAnim = R.anim.slide_in_right;
         outAnim = R.anim.slide_out_left;
      }

      ft.setCustomAnimations(inAnim, outAnim,
       R.anim.slide_in_left, R.anim.slide_out_right);
      ft.replace(R.id.topic_list_fragment, fragment);

      if (addToBack) {
         ft.addToBackStack(null);
      }

      // ft.commit();
      // commitAllowingStateLoss doesn't throw an exception if commit() is
      // run after the fragments parent already saved its state.  Possibly
      // fixes the IllegalStateException crash in FragmentManagerImpl.checkStateLoss()
      ft.commitAllowingStateLoss();
   }

   /////////////////////////////////////////////////////
   // HELPERS
   /////////////////////////////////////////////////////

   @Override
   public void showLoading(int container) {
      mTopicView =
       (TopicViewFragment) getSupportFragmentManager().findFragmentById(R.id.topic_view_fragment);
      getSupportFragmentManager().beginTransaction()
       .add(R.id.topic_list_fragment, new LoadingFragment(), LOADING).addToBackStack(LOADING)
       .commit();
      if (mTopicView != null) {
         getSupportFragmentManager().beginTransaction().hide(mTopicView).addToBackStack(null).commit();
      }
   }

   @Override
   public void hideLoading() {
      getSupportFragmentManager().popBackStack(LOADING, FragmentManager.POP_BACK_STACK_INCLUSIVE);
   }
}
