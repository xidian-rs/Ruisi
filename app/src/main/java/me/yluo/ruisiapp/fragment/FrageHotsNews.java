package me.yluo.ruisiapp.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.yluo.ruisiapp.App;
import me.yluo.ruisiapp.R;
import me.yluo.ruisiapp.adapter.BaseAdapter;
import me.yluo.ruisiapp.adapter.HotNewListAdapter;
import me.yluo.ruisiapp.adapter.PostListAdapter;
import me.yluo.ruisiapp.database.MyDB;
import me.yluo.ruisiapp.listener.LoadMoreListener;
import me.yluo.ruisiapp.model.ArticleListData;
import me.yluo.ruisiapp.model.GalleryData;
import me.yluo.ruisiapp.myhttp.HttpUtil;
import me.yluo.ruisiapp.myhttp.ResponseHandler;
import me.yluo.ruisiapp.myhttp.SyncHttpClient;
import me.yluo.ruisiapp.utils.DimenUtils;
import me.yluo.ruisiapp.utils.GetId;
import me.yluo.ruisiapp.widget.MyListDivider;

/**
 * Created by yang on 16-3-19.
 * 简单的fragment 首页第二页 展示最新的帖子等
 */
public class FrageHotsNews extends BaseLazyFragment implements LoadMoreListener.OnLoadMoreListener {

    private static final int TYPE_HOT = 0;
    private static final int TYPE_NEW = 1;

    private int currentType = 1;
    protected RecyclerView postList;
    protected SwipeRefreshLayout refreshLayout;
    private final List<GalleryData> galleryDatas = new ArrayList<>();
    private final List<ArticleListData> mydataset = new ArrayList<>();
    private HotNewListAdapter adapter;
    private boolean isEnableLoadMore = false;
    private int currentPage = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ((RadioButton) mRootView.findViewById(R.id.btn_1)).setText("新帖");
        mRootView.findViewById(R.id.btn_2).setVisibility(View.GONE);
        ((RadioButton) mRootView.findViewById(R.id.btn_3)).setText("热帖");

        postList = mRootView.findViewById(R.id.recycler_view);
        refreshLayout = mRootView.findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(R.color.red_light, R.color.green_light, R.color.blue_light, R.color.orange_light);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        postList.setLayoutManager(mLayoutManager);
        postList.addItemDecoration(new MyListDivider(getActivity(), MyListDivider.VERTICAL));
        //设置可以滑出底栏
        postList.setClipToPadding(false);
        postList.setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.bottombarHeight));
        adapter = new HotNewListAdapter(getActivity(), mydataset, galleryDatas);
        postList.setAdapter(adapter);
        postList.addOnScrollListener(new LoadMoreListener(mLayoutManager, this, 10));
        refreshLayout.setOnRefreshListener(this::refresh);

        RadioGroup swictchMes = mRootView.findViewById(R.id.btn_change);
        swictchMes.setOnCheckedChangeListener((radioGroup, id) -> {
            int pos = -1;
            if (id == R.id.btn_1) {
                pos = TYPE_NEW;
            } else {
                pos = TYPE_HOT;
            }

            if (pos != currentType) {
                currentType = pos;
                refreshLayout.setRefreshing(true);
                refresh();
            }
        });
        return mRootView;
    }

    @Override
    public void onFirstUserVisible() {
        getData();
    }

    @Override
    public void scrollToTop() {
        if (mydataset.size() > 0) {
            int offset = postList.computeVerticalScrollOffset();
            if (offset == 0) {
                refresh();
            } else if (offset > DimenUtils.getScreenHeight() * 4) {
                postList.scrollToPosition(0);
            } else {
                postList.smoothScrollToPosition(0);
            }
        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_msg_hot;
    }

    private void refresh() {
        currentPage = 1;
        getData();
    }

    @Override
    public void onLoadMore() {
        if (isEnableLoadMore) {
            currentPage++;
            getData();
        }
    }

    private void getData() {
        isEnableLoadMore = false;
        refreshLayout.setRefreshing(true);
        adapter.changeLoadMoreState(BaseAdapter.STATE_LOADING);
        if (App.IS_SCHOOL_NET) {
            new GetGalleryTask().execute();
        }
        String type = (currentType == TYPE_HOT) ? "hot" : "new";
        String url = "forum.php?mod=guide&view=" + type + "&page=" + currentPage + "&mobile=2";
        HttpUtil.get(url, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                new GetNewArticleListTaskMe().execute(new String(response));
            }

            @Override
            public void onFailure(Throwable e) {
                if (e != null && e == SyncHttpClient.NeedLoginError) {
                    adapter.setLoadFailedText("需要登陆");
                }
                refreshLayout.postDelayed(() -> refreshLayout.setRefreshing(false), 300);
                adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_FAIL);
            }
        });
    }

    private class GetGalleryTask extends AsyncTask<Void, Void, List<GalleryData>> {
        @Override
        protected List<GalleryData> doInBackground(Void... voids) {
            List<GalleryData> temps = new ArrayList<>();
            String url = App.BASE_URL_RS + "forum.php";
            Document doc;
            try {
                doc = Jsoup.connect(url).userAgent(SyncHttpClient.DEFAULT_USER_AGENT).get();
            } catch (IOException e) {
                e.printStackTrace();
                return temps;
            }

            Elements listgallerys = doc.select("#wp").select("ul.slideshow");
            for (Element e : listgallerys.select("li")) {
                String title = e.text();
                String titleurl = e.select("a").attr("href");
                String imgurl = e.select("img").attr("src");
                temps.add(new GalleryData(imgurl, title, titleurl));
            }
            return temps;
        }

        @Override
        protected void onPostExecute(List<GalleryData> data) {
            super.onPostExecute(data);
            if (data.size() == 0) {
                return;
            }
            if (galleryDatas.size() == 0) {
                galleryDatas.addAll(data);
            } else if (galleryDatas.size() != data.size()) {//进行了一下优化 只有不相同时才刷行
                galleryDatas.clear();
                galleryDatas.addAll(data);
            } else {
                return;
            }
            adapter.notifyItemChanged(0);
        }
    }

    private class GetNewArticleListTaskMe extends AsyncTask<String, Void, List<ArticleListData>> {
        @Override
        protected List<ArticleListData> doInBackground(String... params) {
            List<ArticleListData> dataset = new ArrayList<>();
            Document doc = Jsoup.parse(params[0]);
            Elements body = doc.select("div[class=threadlist]"); // 具有 href 属性的链接
            Elements links = body.select("li");
            for (Element src : links) {
                String url = src.select("a").attr("href");
                Integer titleColor = GetId.getColor(src.select("a").attr("style"));
                if (titleColor == null) {
                    titleColor = ContextCompat.getColor(getActivity(), R.color.text_color_pri);
                }
                String author = src.select(".by").text();
                src.select("span.by").remove();
                String replyCount = src.select("span.num").text();
                src.select("span.num").remove();
                String title = src.select("a").text();
                String img = src.select("img").attr("src");
                PostListAdapter.MobilePostType postType = PostListAdapter.MobilePostType.parse(img);
                dataset.add(new ArticleListData(postType, title, url, author, replyCount, titleColor));
            }

            MyDB myDB = new MyDB(getActivity());
            return myDB.handReadHistoryList(dataset);
        }

        @Override
        protected void onPostExecute(List<ArticleListData> datas) {
            refreshLayout.postDelayed(() -> refreshLayout.setRefreshing(false), 300);
            if (currentPage == 1) {
                mydataset.clear();
                mydataset.addAll(datas);
                adapter.notifyDataSetChanged();
            } else {
                if (datas.size() == 0) {
                    adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_NOTHING);
                    return;
                } else {
                    int size = mydataset.size();
                    mydataset.addAll(datas);
                    adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_OK);
                    if (galleryDatas.size() > 0) {
                        size++;
                    }
                    adapter.notifyItemRangeInserted(size, datas.size());
                }
            }
            isEnableLoadMore = true;
        }
    }

}
