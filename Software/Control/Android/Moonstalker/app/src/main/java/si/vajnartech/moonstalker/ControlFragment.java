package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import si.vajnartech.moonstalker.processor.AstroObj;

public class ControlFragment extends MyFragment
{
    protected Spinner skyObjects;
    protected ArrayAdapter<String> skyObjectAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View res = super.onCreateView(inflater, container, savedInstanceState);
        assert res != null;
        skyObjects = res.findViewById(R.id.aimed_sky_object);
        skyObjects.setVisibility(View.VISIBLE);
        initAstroObjDropDown();

        return res;
    }

    private void initAstroObjDropDown()
    {
        skyObjectAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item);
        for (AstroObj obj: act.astroData.data) {
            skyObjectAdapter.add(obj.name);
        }
        skyObjects.setAdapter(skyObjectAdapter);
    }
}
