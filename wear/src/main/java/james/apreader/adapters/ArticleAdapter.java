package james.apreader.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import james.apreader.R;
import james.apreader.common.Supplier;
import james.apreader.common.data.WallData;

public class ArticleAdapter extends WearableRecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    private Context context;
    private Supplier supplier;
    private List<WallData> articles;
    private Integer artistId;

    public ArticleAdapter(Context context, List<WallData> articles, int artistId) {
        this.context = context;
        supplier = (Supplier) context.getApplicationContext();
        this.articles = articles;
        this.artistId = artistId;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < articles.size())
            return 0;
        else return 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0)
            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_article, parent, false));
        else return new ViewHolder(new View(context));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            WallData article = articles.get(position);
            holder.title.setText(article.name);
            holder.subtitle.setText(article.desc.contains(".") ? article.desc.substring(0, article.desc.indexOf(".") + 1) : article.desc);
        } else {
            if (artistId != null) {
                supplier.getWallpapers(artistId, new Supplier.AsyncListener<ArrayList<WallData>>() {
                    @Override
                    public void onTaskComplete(ArrayList<WallData> value) {
                        articles.addAll(value);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure() {
                        artistId = null;
                        notifyDataSetChanged();

                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return articles.size() + 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView subtitle;

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            subtitle = (TextView) v.findViewById(R.id.subtitle);
        }
    }
}
