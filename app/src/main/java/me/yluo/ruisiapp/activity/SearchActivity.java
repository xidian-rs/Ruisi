package me.yluo.ruisiapp.activity;

import android.animation.Animator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.yluo.ruisiapp.R;
import me.yluo.ruisiapp.adapter.BaseAdapter;
import me.yluo.ruisiapp.adapter.SimpleListAdapter;
import me.yluo.ruisiapp.listener.LoadMoreListener;
import me.yluo.ruisiapp.model.ListType;
import me.yluo.ruisiapp.model.SimpleListData;
import me.yluo.ruisiapp.myhttp.HttpUtil;
import me.yluo.ruisiapp.myhttp.ResponseHandler;
import me.yluo.ruisiapp.utils.GetId;
import me.yluo.ruisiapp.utils.KeyboardUtil;
import me.yluo.ruisiapp.widget.MyListDivider;

/**
 * Created by yang on 16-4-6.
 * 搜索activity
 * 搜索换页目的是获得searchid这个参数，然后加上page 参数即可
 * http://bbs.rs.xidian.me/search.php?mod=forum&amp;searchid=1268&amp;
 * orderby=lastpost&amp;ascdesc=desc&amp;searchsubmit=yes&amp;page=20&amp;mobile=2
 * <p>
 * http://bbs.rs.xidian.me/search.php?mod=forum&searchid=865&orderby=lastpost&ascdesc=desc&searchsubmit=yes&kw=%E6%B5%8B%E8%AF%95&mobile=2
 * todo 加入热搜 接口地址 http://rs.xidian.edu.cn/misc.php?mod=tag&inajax=1
 */
public class SearchActivity extends BaseActivity implements LoadMoreListener.OnLoadMoreListener,
        View.OnClickListener, EditText.OnEditorActionListener {

    private int totalPage = 1;
    private int currentPage = 1;
    private String searchid = "";
    private boolean isEnableLoadMore = false;
    private SimpleListAdapter adapter;
    private final List<SimpleListData> datas = new ArrayList<>();
    private EditText searchInput;
    private CardView searchCard;
    private Animator animator;
    private TextView navTitle;
    private View mainWindow;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mainWindow = findViewById(R.id.main_window);
        findViewById(R.id.btn_back).setOnClickListener(this);
        RecyclerView listView = findViewById(R.id.recycler_view);
        searchInput = findViewById(R.id.search_input);
        searchCard = findViewById(R.id.search_card);
        findViewById(R.id.start_search).setOnClickListener(this);
        findViewById(R.id.nav_search).setOnClickListener(this);
        searchInput.setHint("请输入搜索内容！");
        adapter = new SimpleListAdapter(ListType.SERRCH, this, datas);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(layoutManager);
        listView.addItemDecoration(new MyListDivider(this, MyListDivider.VERTICAL));
        listView.addOnScrollListener(new LoadMoreListener((LinearLayoutManager) layoutManager, this, 20));
        listView.setAdapter(adapter);
        adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_NOTHING);
        navTitle = findViewById(R.id.nav_title);
        findViewById(R.id.nav_back).setOnClickListener(this);
        searchInput.setOnEditorActionListener(this);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        boolean handled = false;
        if (i == EditorInfo.IME_ACTION_SEARCH) {
            startSearchClick();
            handled = true;
        }
        return handled;
    }

    @Override
    protected void onStart() {
        super.onStart();
        searchCard.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                showSearchView();
            }
        });
    }

    private void startSearchClick() {
        String str = searchInput.getText().toString();
        if (TextUtils.isEmpty(str)) {
            Snackbar.make(mainWindow, "你还没写内容呢", Snackbar.LENGTH_SHORT).show();
            return;
        } else {
            navTitle.setText("搜索:" + str);
            hideSearchView();
            getData(str);
        }
        KeyboardUtil.hideKeyboard(this);
        datas.clear();
        adapter.notifyDataSetChanged();
        isEnableLoadMore = true;
        searchid = "";
    }

    private void getData(String str) {
        adapter.changeLoadMoreState(BaseAdapter.STATE_LOADING);
        String url = "search.php?mod=forum&mobile=2";
        Map<String, String> paras = new HashMap<>();
        paras.put("searchsubmit", "yes");
        paras.put("srchtxt", str);

        HttpUtil.post(url, paras, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                String res = new String(response);
                if (res.contains("秒内只能进行一次搜索")) {
                    getDataFail("抱歉，您在 15 秒内只能进行一次搜索");
                } else if (res.contains("没有找到匹配结果")) {
                    getDataFail("对不起，没有找到匹配结果。");
                } else {
                    new GetResultListTaskMe().execute(new String(response));
                }
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
                getDataFail(null);
            }
        });
    }

    private void getSomePageData(int page) {
        String str = searchInput.getText().toString();
        String url = "search.php?mod=forum&searchid=" + searchid
                + "&orderby=lastpost&ascdesc=desc&searchsubmit=yes&kw=" + str
                + "&page=" + page + "&mobile=2";
        HttpUtil.get(url, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                new GetResultListTaskMe().execute(new String(response));
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
                getDataFail(null);
            }
        });
    }

    private void getDataFail(String res) {
        String erreortext = res;
        if (TextUtils.isEmpty(res)) {
            erreortext = "网络错误(Error -2)";
        }
        isEnableLoadMore = true;
        adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_FAIL);
        Snackbar.make(mainWindow, erreortext, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadMore() {
        //loadmore 被出发
        //加载更多
        if (isEnableLoadMore) {
            isEnableLoadMore = false;
            int page = currentPage;
            if (currentPage < totalPage && totalPage > 1 && (!TextUtils.isEmpty(searchid))) {
                Log.i("loadmore", currentPage + "");
                page = page + 1;
                getSomePageData(page);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.nav_back:
            case R.id.btn_back:
                finish();
                break;
            case R.id.nav_search:
                showSearchView();
                break;
            case R.id.start_search:
                startSearchClick();
                break;
            default:
                break;
        }
    }

    private class GetResultListTaskMe extends AsyncTask<String, Void, List<SimpleListData>> {
        private String searchRes = "";

        @Override
        protected List<SimpleListData> doInBackground(String... params) {
            String res = params[0];
            List<SimpleListData> dataset = new ArrayList<>();
            Document doc = Jsoup.parse(res);
            Elements body = doc.select("div[class=threadlist]"); // 具有 href 属性的链接
            searchRes = body.select("h2.thread_tit").text();
            //获得总页数
            //获取总页数 和当前页数
            if (doc.select(".pg").text().length() > 0) {
                Elements pageinfos = doc.select(".pg");
                currentPage = GetId.getNumber(pageinfos.select("strong").text());
                int n = GetId.getNumber(pageinfos.select("span").attr("title"));
                if (n > 0 && n > totalPage) {
                    totalPage = n;
                }
                if (totalPage > 1) {
                    searchid = GetId.getId("searchid=", pageinfos.select("a").attr("href"));
                }
            }

            Elements links = body.select("li");
            for (Element src : links) {
                String url = src.select("a").attr("href");
                String title = src.select("a").html();
                dataset.add(new SimpleListData(title, "", url));
            }
            return dataset;
        }

        @Override
        protected void onPostExecute(List<SimpleListData> dataset) {
            isEnableLoadMore = true;
            if (!TextUtils.isEmpty(searchRes) && currentPage == 1) {
                navTitle.setText(searchRes.substring(3));
            }
            if (dataset.size() == 0) {
                adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_NOTHING);
            } else {
                if (currentPage >= totalPage) {
                    adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_NOTHING);
                    isEnableLoadMore = false;
                }
                int start = datas.size();
                datas.addAll(dataset);
                if (start == 0) {
                    adapter.notifyDataSetChanged();
                }
                adapter.notifyItemRangeInserted(start, dataset.size());
            }
        }
    }

    private void showSearchView() {
        searchCard.setVisibility(View.VISIBLE);
        animator = ViewAnimationUtils.createCircularReveal(
                searchCard,
                searchCard.getWidth(),
                0,
                0,
                (float) Math.hypot(searchCard.getWidth(), searchCard.getHeight()));

        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(260);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                KeyboardUtil.showKeyboard(searchInput);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void hideSearchView() {
        animator = ViewAnimationUtils.createCircularReveal(
                searchCard,
                searchCard.getWidth(),
                0,
                (float) Math.hypot(searchCard.getWidth(), searchCard.getHeight()),
                0);

        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(260);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                searchCard.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

    }
}
