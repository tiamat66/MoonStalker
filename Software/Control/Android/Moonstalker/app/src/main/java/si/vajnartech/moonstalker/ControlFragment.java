package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import si.vajnartech.moonstalker.processor.AstroObj;
import si.vajnartech.moonstalker.processor.CelestialObj;

public class ControlFragment extends MyFragment
{
    private Spinner skyObjects;
    private View detailsPanel;
    private TextView valueRa;
    private TextView valueDec;

    ArrayAdapter<String> skyObjectAdapter = new ArrayAdapter<>(act, R.layout.spinner_item);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View res = super.onCreateView(inflater, container, savedInstanceState);
        if (res == null) return null;

        skyObjects = res.findViewById(R.id.aimed_sky_object);
        detailsPanel = res.findViewById(R.id.object_details_panel);
        valueRa = res.findViewById(R.id.value_ra);
        valueDec = res.findViewById(R.id.value_dec);

        res.findViewById(R.id.label_aimed_object).setVisibility(View.VISIBLE);
        res.findViewById(R.id.spinner_container).setVisibility(View.VISIBLE);
        skyObjects.setVisibility(View.VISIBLE);
        detailsPanel.setVisibility(View.VISIBLE);

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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
    }

    private void updateDetails(int position)
    {
        String name = skyObjectAdapter.getItem(position);
        if (act.objectsDatabase != null && act.objectsDatabase.data != null && position < act.objectsDatabase.data.size()) {
            CelestialObj selected = act.objectsDatabase.data.get(name);
            String sRa = selected.ra.hours + ":" + selected.ra.minutes + ":" + selected.ra.seconds;
            String sDec = selected.dec.degrees + ":" + selected.dec.minutes + ":" + selected.dec.seconds;
            valueRa.setText(sRa);
            valueDec.setText(sDec);
        }
    }
}
