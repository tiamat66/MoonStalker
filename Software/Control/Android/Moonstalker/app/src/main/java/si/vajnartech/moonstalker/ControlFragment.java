package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import si.vajnartech.moonstalker.processor.CelestialObj;

public class ControlFragment extends MyFragment
{
    private Spinner skyObjects;
    private TextView valueRa;
    private TextView valueDec;
    private TextView valueAzimuth;
    private TextView valueElevation;
    private TelescopeView telescopeViz;
    private volatile double elevation = 0;
    private volatile double azimuth = 0;

    ArrayAdapter<String> skyObjectAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View res = super.onCreateView(inflater, container, savedInstanceState);
        if (res == null) return null;

        skyObjectAdapter = new ArrayAdapter<>(act, R.layout.spinner_item);

        skyObjects = res.findViewById(R.id.aimed_sky_object);
        View detailsPanel = res.findViewById(R.id.object_details_panel);
        valueRa = res.findViewById(R.id.value_ra);
        valueDec = res.findViewById(R.id.value_dec);
        valueAzimuth = res.findViewById(R.id.value_azimuth);
        valueElevation = res.findViewById(R.id.value_elevation);
        telescopeViz = res.findViewById(R.id.telescope_viz);

        res.findViewById(R.id.label_aimed_object).setVisibility(View.VISIBLE);
        res.findViewById(R.id.spinner_container).setVisibility(View.VISIBLE);
        skyObjects.setVisibility(View.VISIBLE);
        detailsPanel.setVisibility(View.VISIBLE);
        if (telescopeViz != null) telescopeViz.setVisibility(View.VISIBLE);

        initAstroObjDropDown();

        return res;
    }

    private void initAstroObjDropDown()
    {
        // Using custom layout for better visibility in dark theme
        skyObjectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        if (act.objectsDatabase != null && act.objectsDatabase.data != null) {
            for (String name : act.objectsDatabase.data.keySet()) {
                skyObjectAdapter.add(name);
            }
        }
        skyObjects.setAdapter(skyObjectAdapter);

        skyObjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                updateDetails(position);
                act.toObjName = skyObjectAdapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        int position = skyObjectAdapter.getPosition(act.curObjName);
        skyObjects.setSelection(position);
    }

    private void updateDetails(int position)
    {
        if (act.objectsDatabase != null && act.objectsDatabase.data != null && position < act.objectsDatabase.data.size()) {
            String name = skyObjectAdapter.getItem(position);
            CelestialObj selected = act.objectsDatabase.data.get(name);
            assert selected != null;
            String sRa = selected.ra.hours + "h " + selected.ra.minutes + "' " + selected.ra.seconds + "\"";
            String sDec = selected.dec.degrees + "°" + selected.dec.minutes + "' " + selected.dec.seconds + "\"";

            valueRa.setText(sRa);
            valueDec.setText(sDec);

            valueAzimuth.setText(String.format("%.2f°", azimuth));
            valueElevation.setText(String.format("%.2f°", elevation));

            if (telescopeViz != null) {
                telescopeViz.update((float) azimuth, (float) elevation);
            }
        }
    }

    public void update(double elevation, double azimuth)
    {
        this.elevation = elevation;
        this.azimuth = azimuth;
        updateDetails(skyObjects.getSelectedItemPosition());
    }
}
