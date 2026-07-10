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

public class ControlFragment extends MyFragment
{
    private Spinner skyObjects;
    private View detailsPanel;
    private TextView valueRa;
    private TextView valueDec;

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
        ArrayAdapter<String> skyObjectAdapter = new ArrayAdapter<>(act, R.layout.spinner_item);
        skyObjectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        if (act.astroData != null && act.astroData.data != null) {
            for (AstroObj obj : act.astroData.data) {
                skyObjectAdapter.add(obj.name);
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
        if (act.astroData != null && act.astroData.data != null && position < act.astroData.data.size()) {
            AstroObj selected = act.astroData.data.get(position);
            valueRa.setText(selected.ra);
            valueDec.setText(selected.dec);
        }
    }
}
