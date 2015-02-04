package org.numixproject.hermes;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.receiver.ServerReceiver;


public class HomeFragment extends Fragment {

    private SlidingUpPanelLayout serverSliding = null;
    private IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;
    private ListView list;
    private static int instanceCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout llLayout = (FrameLayout) inflater.inflate(R.layout.server_sliding_fragment, container, false);

        // Inflate the layout for this fragment
        llLayout.findViewById(R.id.home_mainFragment);

        serverSliding = (SlidingUpPanelLayout) llLayout.findViewById(R.id.sliding_layout);
        serverSliding.setEnableDragViewTouchEvents(true);
        list = (ListView) llLayout.findViewById(android.R.id.list);
        list.setAdapter(adapter);
        return llLayout;
    }
    public void openServerPane() {
        serverSliding.expandPanel();
    }

}