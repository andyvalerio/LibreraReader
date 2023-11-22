package com.foobnix.pdf.search.activity;

import android.graphics.Matrix;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.greenrobot.eventbus.EventBus;

class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener {

    private final PageView pageView;

    public SimpleGestureListener(PageView pageView) {
        this.pageView = pageView;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        pageView.clickUtils.init();
        pageView.setIgronerClick(true);
        if (pageView.clickUtils.isClickCenter(e.getX(), e.getY())) {
            pageView.setLongPress(true);

            if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_NOTHING) {

            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_ZOOM_IN_OUT) {
                if (PageView.isFirstZoomInOut) {
                    pageView.imageMatrix().preTranslate(pageView.getWidth() / 2 - e.getX(), pageView.getHeight() / 2 - e.getY());
                    pageView.imageMatrix().postScale(2.5f, 2.5f, pageView.getWidth() / 2, pageView.getHeight() / 2);
                    PageView.isFirstZoomInOut = false;
                    PageView.prevLock = AppSP.get().isLocked;
                    AppSP.get().isLocked = false;
                    pageView.invalidateAndMsg();
                    PageState.get().isAutoFit = false;

                } else {
                    AppSP.get().isLocked = PageView.prevLock;
                    if (BookCSS.get().isTextFormat()) {
                        AppSP.get().isLocked = true;
                    }
                    pageView.setLongPress(true);
                    PageState.get().isAutoFit = true;
                    pageView.autoFit();
                    pageView.invalidateAndMsg();
                    PageView.isFirstZoomInOut = true;

                }
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK, e.getX(), e.getY()));
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_AUTOSCROLL) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_AUTO_SCROLL));
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK_APP, e.getX(), e.getY()));
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_HIDE_APP) {
                Apps.showDesctop(pageView.getContext());
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_START_STOP_TTS) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PLAY_PAUSE, e.getX(), e.getY()));
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CENTER_HORIZONTAL) {
                PageState.get().isAutoFit = false;
                pageView.onCenterHorizontally(new MessageCenterHorizontally(pageView.getPageNumber()));
            } else {
                PageState.get().isAutoFit = true;
                pageView.autoFit();
                pageView.invalidateAndMsg();
            }

            EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_DOUBLE_TAP, e.getX(), e.getY()));
            return true;
        }

        return true;
    }

    ;

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {

        if (e1.getX() < BrightnessHelper.BRIGHTNESS_WIDTH) {
            return false;
        }
        if (AppState.get().selectedText != null) {
            return false;
        }
        if (AppSP.get().isLocked) {
            return false;
        }
        if (pageView.isReadyForMove()) {
            pageView.setIgronerClick(true);
            pageView.scroller.fling((int) e2.getX(), (int) e2.getY(), (int) velocityX / 3, (int) velocityY / 3, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            pageView.getHandler().post(pageView.scrolling);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        pageView.setIgronerClick(true);

        if (AppState.get().isSelectTexByTouch) {
            //EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, e.getX(), e.getY()));
            pageView.setLongPress(false);
            pageView.setIgronerClick(true);
            AppState.get().selectedText = null;
            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
            if (new ClickUtils().isClickCenter(e.getX(), e.getY())) {
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, e.getX(), e.getY()));
            }
            LOG.d("PageImageView MESSAGE_PERFORM_CLICK", 3);


            return;
        }

        if (!AppState.get().isAllowTextSelection) {
            Toast.makeText(LibreraApp.context, R.string.text_highlight_mode_is_disable, Toast.LENGTH_LONG).show();
            return;
        }

        Vibro.vibrate();
        if (AppSP.get().isCut || AppSP.get().isCrop) {
            Toast.makeText(LibreraApp.context, R.string.the_page_is_clipped_the_text_selection_does_not_work, Toast.LENGTH_LONG).show();
            return;
        }
        pageView.setLongPress(true);
        pageView.xInit = e.getX();
        pageView.yInit = e.getY();
        String selectText = pageView.selectText(pageView.xInit, pageView.yInit, e.getX(), e.getY());
        if (TxtUtils.isEmpty(selectText)) {
            AppState.get().selectedText = null;
            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));

        }
    }

    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            AppState.get().selectedText = null;
            LOG.d("TEST", "action ACTION_DOWN");
            pageView.scroller.forceFinished(true);
            pageView.x = event.getX();
            pageView.y = event.getY();
            pageView.brightnessHelper.onActoinDown(pageView.x, pageView.y);
            pageView.setReadyForMove(false);
            if (AppState.get().isSelectTexByTouch) {
                pageView.setLongPress(true);
                pageView.setIgronerClick(true);
                pageView.xInit = pageView.x;
                pageView.yInit = pageView.y;
            } else {
                pageView.setLongPress(false);
            }
            pageView.setIsMoveNextPrev(0);
            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() == 1) {
                LOG.d("TEST", "action ACTION_MOVE 1");
                final float dx = event.getX() - pageView.x;
                final float dy = event.getY() - pageView.y;

                if (pageView.isLongPress()) {
                    String selectText = pageView.selectText(event.getX(), event.getY(), pageView.xInit, pageView.yInit);
                    if (selectText != null && selectText.contains(" ")) {
                        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SHOW, -1, pageView.xInit, pageView.yInit, event.getX(), event.getY()));
                    }
                } else {

                    if (AppSP.get().isLocked) {
                        pageView.setReadyForMove(false);
                        if (AppState.get().isSelectTexByTouch) {
                            pageView.setIgronerClick(true);
                        } else {
                            pageView.setIgronerClick(false);
                        }
                        if (AppState.get().isEnableVerticalSwipe && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Dips.DP_10) {
                            if (AppState.get().isSwipeGestureReverse) {
                                pageView.setIsMoveNextPrev(dy > 0 ? -1 : 1);
                            } else {
                                pageView.setIsMoveNextPrev(dy > 0 ? 1 : -1);
                            }
                        }

                    } else {
                        if (AppState.get().rotateViewPager == 0) {
                            if (Math.abs(dy) > Math.abs(dx) && (Math.abs(dy) + Math.abs(dx) > Dips.DP_10)) {
                                pageView.setReadyForMove(true);
                                pageView.setIgronerClick(true);
                            }
                        } else {
                            if (Math.abs(dx) > Math.abs(dy) && (Math.abs(dx) + Math.abs(dy) > Dips.DP_10)) {
                                pageView.setReadyForMove(true);
                                pageView.setIgronerClick(true);
                            }
                        }
                    }

                    boolean isBrightness = pageView.brightnessHelper.onActionMove(event);
                    if (isBrightness) {
                        pageView.setIgronerClick(true);
                        pageView.setIsMoveNextPrev(0);
                    }

                    if (!isBrightness && pageView.isReadyForMove() && !AppSP.get().isLocked) {

                        pageView.imageMatrix().postTranslate(dx, dy);

                        PageState.get().isAutoFit = false;
                        pageView.invalidateAndMsg();

                        pageView.x = event.getX();
                        pageView.y = event.getY();
                    }

                }

            }

            if (event.getPointerCount() == 2) {
                pageView.setIgronerClick(true);

                LOG.d("TEST", "action ACTION_MOVE 2");
                if (pageView.cx == 0) {
                    pageView.cx = pageView.centerX(event);
                    pageView.cy = pageView.centerY(event);
                }
                final float nDistance = pageView.distance(event);

                if (pageView.distance == 0) {
                    pageView.distance = nDistance;
                }

                final float scale = nDistance / pageView.distance;
                pageView.distance = nDistance;
                final float centerX = pageView.centerX(event);
                final float centerY = pageView.centerY(event);

                final float values[] = new float[9];
                pageView.imageMatrix().getValues(values);

                if (AppState.get().isZoomInOutWithLock || !AppSP.get().isLocked) {
                    LOG.d("postScale", scale, values[Matrix.MSCALE_X]);
                    if (values[Matrix.MSCALE_X] > 0.3f || scale > 1) {
                        pageView.imageMatrix().postScale(scale, scale, centerX, centerY);
                        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                    }
                }
                final float dx = centerX - pageView.cx;
                final float dy = centerY - pageView.cy;
                if (AppState.get().isZoomInOutWithLock || !AppSP.get().isLocked) {
                    pageView.imageMatrix().postTranslate(dx, dy);
                    EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                }
                pageView.cx = pageView.centerX(event);
                pageView.cy = pageView.centerY(event);

                PageState.get().isAutoFit = false;
                pageView.invalidateAndMsg();

            }
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            LOG.d("TEST", "action ACTION_POINTER_UP");
            // isDoubleTouch = true;
            int actionIndex = event.getActionIndex();
            LOG.d("TEST", "actionIndex " + actionIndex);
            if (actionIndex == 1) {
                pageView.x = event.getX();
                pageView.y = event.getY();
            } else {
                pageView.x = event.getX(1);
                pageView.y = event.getY(1);
            }
            pageView.cx = 0;
            pageView.distance = 0;

        } else if (action == MotionEvent.ACTION_UP) {
            pageView.brightnessHelper.onActionUp();

            LOG.d("TEST", "action ACTION_UP", "long: " + pageView.isLongPress());
            pageView.distance = 0;
            pageView.setReadyForMove(false);
            pageView.cx = 0;
            pageView.cy = 0;

            if (pageView.isLongPress()) {
                String selectText = pageView.selectText(event.getX(), event.getY(), pageView.xInit, pageView.yInit);
                if (selectText != null && selectText.contains(" ")) {
                    EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SHOW, -1, pageView.xInit, pageView.yInit, event.getX(), event.getY()));
                }
                // If you moved more than 10px left or right, swipe the page
                final float dx = event.getX() - pageView.xInit;
                if (Math.abs(dx) > 10) {
                    // This is a swipe so scroll the page
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE,
                            dx > 0 ? -1 : 1));
                }


            } else if (BookCSS.get().isTextFormat()) {
                if (!TempHolder.isSeaching) {
                    pageView.selectText(event.getX(), event.getY(), event.getX(), event.getY());
                    if (!TxtUtils.isFooterNote(AppState.get().selectedText)) {
                        PageState.get().cleanSelectedWords();
                        AppState.get().selectedText = null;
                        pageView.invalidate();

                        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                    }
                }
            }

            if (!pageView.isIgronerClick()) {
                int target = 0;
                Pair<PageLink, Annotation> pair = pageView.getPageLinkClicked(event.getX(), event.getY());
                PageLink pageLink = pair.first;
                if (pageLink != null) {
                    target = pageLink.targetPage;
                    if (AppSP.get().isDouble && target != -1) {
                        target = pageLink.targetPage / 2;
                    }
                    TempHolder.get().linkPage = target;
                    LOG.d("Go to targetPage", target);
                }
                if (pair.second != null) {
                    Dialogs.showTextDialog(pageView.getContext(), pair.second.text);
                }

                if (pageView.getIsMoveNextPrev() != 0) {
                    LOG.d("isMoveNextPrev", pageView.getIsMoveNextPrev());
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE, pageView.getIsMoveNextPrev()));
                } else if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                } else if (pageLink != null) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_BY_LINK, target, pageLink.url));
                } else {
                    LOG.d("PageImageView MESSAGE_PERFORM_CLICK", 1);
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, event.getX(), event.getY()));
                }
            } else {

                if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                } else if (AppState.get().isSelectTexByTouch && !new ClickUtils().isClickCenter(event.getX(), event.getY())) {
                    LOG.d("PageImageView MESSAGE_PERFORM_CLICK", 2);
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, event.getX(), event.getY()));
                }


            }

            pageView.setIgronerClick(false);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LOG.d("TEST", "action ACTION_CANCEL");
        }

        return true;
    }

}
