package james.apreader.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import james.apreader.R;
import james.apreader.activities.WallActivity;
import james.apreader.common.Supplier;
import james.apreader.common.data.WallData;
import james.apreader.common.utils.FontUtils;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    Activity activity;
    ArrayList<WallData> totalWalls, walls;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View v, title, subtitle, clicker, card;

        public ViewHolder(View v, View title, View subtitle, View clicker, View card) {
            super(v);
            this.v = v;
            this.title = title;
            this.subtitle = subtitle;
            this.clicker = clicker;
            this.card = card;
        }
    }

    public SearchAdapter(Activity activity) {
        Supplier supplier = (Supplier) activity.getApplicationContext();

        this.activity = activity;
        totalWalls = supplier.getWallpapers();
        walls = new ArrayList<>();
    }

    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new ViewHolder(v, v.findViewById(R.id.title), v.findViewById(R.id.subtitle), v.findViewById(R.id.root), v.findViewById(R.id.card));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        TextView title = (TextView) holder.title, subtitle = (TextView) holder.subtitle;

        title.setText(walls.get(position).name);
        subtitle.setText(walls.get(position).authorName);

        FontUtils.applyTypeface(title);
        FontUtils.applyTypeface(subtitle);

        holder.clicker.setTag(position);
        holder.clicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activity, WallActivity.class);
                i.putExtra("wall", walls.get(holder.getAdapterPosition()));
                activity.startActivity(i);
            }
        });

        holder.v.setAlpha(0);
        holder.v.animate().alpha(1).start();
    }

    @Override
    public int getItemCount() {
        return walls.size();
    }

    public void filter(String filter) {
        walls.clear();

        for (WallData data : totalWalls) {
            if (data.name.toLowerCase().contains(filter.toLowerCase()) || filter.toLowerCase().contains(data.name.toLowerCase()))
                walls.add(data);
        }

        notifyDataSetChanged();
    }
}
