package com.bitlove.fetlife.view.activity.resource;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import com.bitlove.fetlife.R;
import com.bitlove.fetlife.event.ServiceCallFailedEvent;
import com.bitlove.fetlife.event.ServiceCallFinishedEvent;
import com.bitlove.fetlife.event.ServiceCallStartedEvent;
import com.bitlove.fetlife.model.service.FetLifeApiIntentService;
import com.bitlove.fetlife.view.activity.component.MenuActivityComponent;
import com.bitlove.fetlife.view.adapter.ResourceListRecyclerAdapter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class ResourceListActivity<Resource> extends ResourceActivity implements MenuActivityComponent.MenuActivityCallBack {

    private static final int PAGE_COUNT = 25;

    protected FloatingActionButton floatingActionButton;
    protected RecyclerView recyclerView;
    protected ResourceListRecyclerAdapter<Resource, ?> recyclerAdapter;

    protected SwipeRefreshLayout swipeRefreshLayout;
    protected LinearLayoutManager recyclerLayoutManager;
    protected View inputLayout;
    protected View inputIcon;
    protected EditText textInput;

    protected int requestedItems = 0;
    protected int requestedPage = 1;

    @Override
    @CallSuper
    protected void onResourceCreate(Bundle savedInstanceState) {

        //TODO: consider removing this
        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setVisibility(View.GONE);

        inputLayout = findViewById(R.id.text_input_layout);
        inputIcon = findViewById(R.id.text_send_icon);
        textInput = (EditText) findViewById(R.id.text_input);
//        textInput.setFilters(new InputFilter[]{new InputFilter() {
//            @Override
//            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
//                  //Custom Emoji Support will go here
//        }});

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(recyclerLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerAdapter = createRecyclerAdapter(savedInstanceState);
        recyclerAdapter.setOnItemClickListener(new ResourceListRecyclerAdapter.OnResourceClickListener<Resource>() {
            @Override
            public void onItemClick(Resource resource) {
                ResourceListActivity.this.onItemClick(resource);
            }

            @Override
            public void onAvatarClick(Resource resource) {
                ResourceListActivity.this.onAvatarClick(resource);
            }
        });
        recyclerView.setAdapter(recyclerAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestedItems = PAGE_COUNT;
                requestedPage = 1;
                String apiAction = getApiCallAction();
                if (apiAction != null) {
                    FetLifeApiIntentService.startApiCall(ResourceListActivity.this, getApiCallAction(), Integer.toString(PAGE_COUNT));
                }
            }
        });

        String apiCallAction = getApiCallAction();
        if (apiCallAction != null) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0) {
                        int visibleItemCount = recyclerLayoutManager.getChildCount();
                        int pastVisibleItems = recyclerLayoutManager.findFirstVisibleItemPosition();
                        int lastVisiblePosition = visibleItemCount + pastVisibleItems;

                        if (lastVisiblePosition >= requestedItems) {
                            requestedItems += PAGE_COUNT;
                            startResourceCall(PAGE_COUNT, ++requestedPage);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onSetContentView() {
        setContentView(R.layout.activity_resource_list);
    }

    @Override
    @CallSuper
    protected void onResourceStart() {
        recyclerAdapter.refresh();

        String apiCallAction = getApiCallAction();
        if (apiCallAction != null) {
            showProgress();
            if (!FetLifeApiIntentService.isActionInProgress(apiCallAction)) {
                startResourceCall(PAGE_COUNT);
            }
            requestedPage = 1;
            requestedItems = PAGE_COUNT;
        }
    }

    private static final int DEFAULT_REQUESTED_PAGE = Integer.MIN_VALUE;

    protected void startResourceCall(int pageCount) {
        startResourceCall(pageCount, DEFAULT_REQUESTED_PAGE);
    }

    protected void startResourceCall(int pageCount, int requestedPage) {
        String apiCallAction = getApiCallAction();
        if (apiCallAction == null) {
            return;
        }
        if (requestedPage != DEFAULT_REQUESTED_PAGE) {
            FetLifeApiIntentService.startApiCall(ResourceListActivity.this, apiCallAction, Integer.toString(pageCount), Integer.toString(requestedPage));
        } else {
            FetLifeApiIntentService.startApiCall(ResourceListActivity.this, apiCallAction, Integer.toString(pageCount));
        }
    }

    protected abstract String getApiCallAction();

    protected abstract ResourceListRecyclerAdapter createRecyclerAdapter(Bundle savedInstanceState);

    public abstract void onItemClick(Resource resource);

    public abstract void onAvatarClick(Resource resource);

    @Override
    public boolean finishAtMenuNavigation() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResourceListCallFinished(ServiceCallFinishedEvent serviceCallFinishedEvent) {
        if (serviceCallFinishedEvent.getServiceCallAction().equals(getApiCallAction())) {
            int receivedItems = serviceCallFinishedEvent.getItemCount();
            if (receivedItems > 0) {
                //One Item we already expected at the call
                requestedItems += receivedItems - PAGE_COUNT;
            }
            recyclerAdapter.refresh();
            dismissProgress();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResourceListCallFailed(ServiceCallFailedEvent serviceCallFailedEvent) {
        if (serviceCallFailedEvent.getServiceCallAction().equals(getApiCallAction())) {
            recyclerAdapter.refresh();
            dismissProgress();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResourceListCallStarted(ServiceCallStartedEvent serviceCallStartedEvent) {
        if (serviceCallStartedEvent.getServiceCallAction().equals(getApiCallAction())) {
            showProgress();
        }
    }
}
