package com.foobnix.pdf.search.activity.handlers;

import androidx.viewpager.widget.ViewPager;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.tts.TTSService;
import com.foobnix.ui2.MainTabs2;

public class MessageEventHandler {

    public void onEvent(MessageEvent ev, HorizontalViewActivity activity) {

        Runnable doShowHideWrapperControllsRunnable = activity::doShowHideWrapperControlls;

        if (activity.getCurrentScrollState() != ViewPager.SCROLL_STATE_IDLE) {
            LOG.d("Skip event");
            return;
        }

        activity.getClickUtils().init();
        LOG.d("MessageEvent", ev.getMessage(), ev.getX(), ev.getY());
        if (ev.getMessage().equals(MessageEvent.MESSAGE_CLOSE_BOOK)) {
            activity.showInterstial();
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_CLOSE_BOOK_APP)) {
            activity.getModeController().onCloseActivityFinal(new Runnable() {

                @Override
                public void run() {
                    MainTabs2.closeApp(activity.getModeController().getActivity());
                }
            });
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_PERFORM_CLICK)) {
            boolean isOpen = activity.closeDialogs();
            if (isOpen) {
                return;
            }

            int x = (int) ev.getX();
            int y = (int) ev.getY();
            if (activity.getClickUtils().isClickRight(x, y) && AppState.get().tapZoneRight != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneRight == AppState.TAP_NEXT_PAGE) {
                    activity.nextPage();
                } else {
                    activity.prevPage();
                }
            } else if (activity.getClickUtils().isClickLeft(x, y) && AppState.get().tapZoneLeft != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneLeft == AppState.TAP_PREV_PAGE) {
                    activity.prevPage();
                } else {
                    activity.nextPage();
                }
            } else if (activity.getClickUtils().isClickTop(x, y) && AppState.get().tapZoneTop != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneTop == AppState.TAP_PREV_PAGE) {
                    activity.prevPage();
                } else {
                    activity.nextPage();
                }

            } else if (activity.getClickUtils().isClickBottom(x, y) && AppState.get().tapZoneBottom != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneBottom == AppState.TAP_NEXT_PAGE) {
                    activity.nextPage();
                } else {
                    activity.prevPage();
                }

            } else {
                LOG.d("Click-center!", x, y);
                activity.getHandler().removeCallbacks(doShowHideWrapperControllsRunnable);
                activity.getHandler().postDelayed(doShowHideWrapperControllsRunnable, 250);
                // Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_DOUBLE_TAP)) {
            activity.getHandler().removeCallbacks(doShowHideWrapperControllsRunnable);
            activity.updateLockMode();
            // Toast.makeText(this, "DB", Toast.LENGTH_SHORT).show();
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_PLAY_PAUSE)) {
            TTSService.playPause(activity, activity.getModeController());
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_SELECTED_TEXT)) {
            if (activity.getModeController().isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
                DragingDialogs.showFootNotes(activity.getAnchor(), activity.getModeController(), activity::showHideHistory);
            } else {
                if (AppState.get().isRememberDictionary) {
                    final String text = AppState.get().selectedText;
                    DictsHelper.runIntent(activity.getModeController().getActivity(), text);
                    activity.getModeController().clearSelectedText();
                } else {
                    DragingDialogs.selectTextMenu(activity.getAnchor(),
                            activity.getModeController(), true, activity.getOnRefresh());
                }
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE_BY_LINK)) {
            if (ev.getPage() == -1 && TxtUtils.isNotEmpty(ev.getBody())) {
                AlertDialogs.openUrl(activity, ev.getBody());
            } else {
                activity.getModeController().getLinkHistory().add(
                        activity.getModeController().getCurentPage() + 1);
                activity.getModeController().onGoToPage(ev.getPage() + 1);
                activity.showHideHistory();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE)) {
            if (ev.getPage() > 0) {
                activity.nextPage();
            } else {
                activity.prevPage();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_AUTO_SCROLL)) {
            if (activity.isFlipping()) {
                activity.onFlippingStop(null);
            } else {
                activity.onFlippingStart(null);
            }

        }
    }


}
