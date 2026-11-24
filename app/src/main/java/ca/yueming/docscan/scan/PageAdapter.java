package ca.yueming.docscan.scan;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ca.yueming.docscan.R;
import ca.yueming.docscan.core.session.ScanSession;
import ca.yueming.docscan.io.Bitmaps;

/** Shows session pages with thumbnail and up/down/delete buttons. */
final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
    interface Listener { void changed(); }

    private final ScanSession session;
    private final Listener listener;

    PageAdapter(ScanSession session, Listener listener) { this.session = session; this.listener = listener; }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView thumb; final TextView label; final View up, down, delete;

        Holder(View v) {
            super(v);
            thumb = v.findViewById(R.id.thumb); label = v.findViewById(R.id.label);
            up = v.findViewById(R.id.up); down = v.findViewById(R.id.down); delete = v.findViewById(R.id.delete);
        }
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        String path = session.pages().get(position);
        Bitmap b = Bitmaps.load(path, 256);
        h.thumb.setImageBitmap(b);
        h.label.setText("Page " + (position + 1));
        h.up.setOnClickListener(v -> { if (session.moveUp(h.getBindingAdapterPosition())) done(); });
        h.down.setOnClickListener(v -> { if (session.moveDown(h.getBindingAdapterPosition())) done(); });
        h.delete.setOnClickListener(v -> { session.remove(h.getBindingAdapterPosition()); done(); });
    }

    private void done() {
        notifyDataSetChanged();
        listener.changed();
    }

    @Override public int getItemCount() { return session.size(); }
}
