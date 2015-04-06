package net.i2p.android.i2ptunnel;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.i2p.android.router.R;

import java.util.List;

public class TunnelEntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mCtx;
    private boolean mClientTunnels;
    private TunnelListFragment.OnTunnelSelectedListener mListener;
    private boolean mIsTwoPane;
    private List<TunnelEntry> mTunnels;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = -1;

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class TunnelViewHolder extends RecyclerView.ViewHolder {
        public ImageView status;
        public TextView name;
        public TextView description;
        public TextView interfacePort;
        public View open;

        public TunnelViewHolder(View itemView) {
            super(itemView);

            status = (ImageView) itemView.findViewById(R.id.tunnel_status);
            name = (TextView) itemView.findViewById(R.id.tunnel_name);
            description = (TextView) itemView.findViewById(R.id.tunnel_description);
            interfacePort = (TextView) itemView.findViewById(R.id.tunnel_interface_port);
            open = itemView.findViewById(R.id.tunnel_open);
        }
    }

    public TunnelEntryAdapter(Context context, boolean clientTunnels,
                              TunnelListFragment.OnTunnelSelectedListener listener,
                              boolean isTwoPane) {
        super();
        mCtx = context;
        mClientTunnels = clientTunnels;
        mListener = listener;
        mIsTwoPane = isTwoPane;
    }

    public void setTunnels(List<TunnelEntry> tunnels) {
        mTunnels = tunnels;
        notifyDataSetChanged();
    }

    public void addTunnel(TunnelEntry tunnel) {
        mTunnels.add(tunnel);
        notifyItemInserted(mTunnels.size()-1);
    }

    public TunnelEntry getTunnel(int position) {
        if (position < 0)
            return null;

        return mTunnels.get(position);
    }

    public void setActivatedPosition(int position) {
        mActivatedPosition = position;
    }

    public int getActivatedPosition() {
        return mActivatedPosition;
    }

    public void clearActivatedPosition() {
        mActivatedPosition = -1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mTunnels == null)
            return R.string.router_not_running;
        else if (mTunnels.isEmpty())
            return R.layout.listitem_empty;
        else
            return R.layout.listitem_i2ptunnel;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int vt = viewType;
        if (viewType == R.string.router_not_running)
            vt = R.layout.listitem_empty;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(vt, parent, false);
        switch (viewType) {
            case R.layout.listitem_i2ptunnel:
                return new TunnelViewHolder(v);
            default:
                return new SimpleViewHolder(v);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case R.string.router_not_running:
                ((TextView) holder.itemView).setText(
                        mCtx.getString(R.string.router_not_running));
                break;

            case R.layout.listitem_empty:
                ((TextView) holder.itemView).setText(
                        mClientTunnels ? "No configured client tunnels." : "No configured server tunnels.");
                break;

            case R.layout.listitem_i2ptunnel:
                final TunnelViewHolder tvh = (TunnelViewHolder) holder;
                final TunnelEntry tunnel = getTunnel(position);

                tvh.status.setImageDrawable(tunnel.getStatusIcon());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                    tvh.status.setBackgroundDrawable(tunnel.getStatusBackground());
                else
                    tvh.status.setBackground(tunnel.getStatusBackground());

                tvh.name.setText(tunnel.getName());
                tvh.description.setText(tunnel.getDescription());
                tvh.interfacePort.setText(tunnel.getTunnelLink(false));

                if (tunnel.isRunning() && tunnel.isTunnelLinkValid()) {
                    tvh.open.setVisibility(View.VISIBLE);
                    tvh.open.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(tunnel.getTunnelLink(true)));
                            try {
                                mCtx.startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
                                builder.setTitle(R.string.install_recommended_app)
                                        .setMessage(R.string.app_needed_for_this_tunnel_type)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Uri uri = tunnel.getRecommendedAppForTunnel();
                                                if (uri != null) {
                                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                                    mCtx.startActivity(intent);
                                                }
                                            }
                                        })
                                        .setNegativeButton(net.i2p.android.lib.client.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                            }
                                        });
                                builder.show();
                            }
                        }
                    });
                } else
                    tvh.open.setVisibility(View.GONE);

                tvh.itemView.setSelected(mIsTwoPane && position == mActivatedPosition);
                tvh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mActivatedPosition = position;
                        notifyItemChanged(position);
                        mListener.onTunnelSelected(tunnel.getId());
                    }
                });
                break;

            default:
                break;
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mTunnels == null || mTunnels.isEmpty())
            return 1;

        return mTunnels.size();
    }
}
