package xyz.yluo.ruisiapp.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import xyz.yluo.ruisiapp.R;
import xyz.yluo.ruisiapp.activity.ArticleListActivity;
import xyz.yluo.ruisiapp.data.FroumListData;
import xyz.yluo.ruisiapp.utils.GetFroumLogo;
import xyz.yluo.ruisiapp.utils.getFroumFid;

/**
 * Created by free2 on 16-3-19.
 *
 */
public class ForumListAdapter extends RecyclerView.Adapter<ForumListAdapter.BaseViewHolder>{

    //数据
    private List<FroumListData> DataSet;
    protected Activity activity;

    private final int TYPE_NORMAL = 0;
    private final int TYPE_HEADER = 1;

    public ForumListAdapter(Activity activity, List<FroumListData> dataSet) {
        DataSet = dataSet;
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {
        if(DataSet.get(position).isheader()){
            return TYPE_HEADER;
        }else{
            return TYPE_NORMAL;
        }
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType==TYPE_NORMAL){
            return new FroumsListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_forums_list_item, parent, false));
        }else{
            return new FroumsListHeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_forums_list_item_header, parent, false));
        }

    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        //set data here
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return DataSet.size();
    }


    public abstract class BaseViewHolder extends RecyclerView.ViewHolder{

        public BaseViewHolder(View itemView) {
            super(itemView);
        }
        abstract void setData(int position);
    }

    //首页板块列表ViewHolder
    public class FroumsListViewHolder extends BaseViewHolder{

        @Bind(R.id.img)
        protected ImageView img;
        @Bind(R.id.title)
        protected TextView title;
        @Bind(R.id.today_count)
        protected TextView today_count;

        public FroumsListViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            today_count.setVisibility(View.GONE);
        }
        @OnClick(R.id.forum_list_item)
        protected void forum_list_item_click(){
            String fid =getFroumFid.getFid(DataSet.get(getAdapterPosition()).getTitleUrl());
            ArticleListActivity.open(activity, Integer.parseInt(fid), DataSet.get(getAdapterPosition()).getTitle());
        }
        void setData(int position) {
            title.setText(DataSet.get(position).getTitle());
            if(DataSet.get(position).getTodayNew()!=""){
                today_count.setVisibility(View.VISIBLE);
                today_count.setText(DataSet.get(position).getTodayNew());
            }

            String url = DataSet.get(position).getTitleUrl();
            Drawable dra = GetFroumLogo.getlogo(activity, url);
            img.setImageDrawable(dra);
        }

    }

    public class FroumsListHeaderViewHolder extends BaseViewHolder{

        @Bind(R.id.header_title)
        protected TextView header_title;

        public FroumsListHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        @Override
        void setData(int position) {
            header_title.setText(DataSet.get(position).getTitle());
        }

        @OnClick(R.id.forum_list_item)
        protected void forum_list_item_click(){

        }
    }

}