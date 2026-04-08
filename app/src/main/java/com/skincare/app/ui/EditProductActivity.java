package com.skincare.app.ui;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.skincare.app.R;
import com.skincare.app.data.*;

public class EditProductActivity extends AppCompatActivity {
    AppData db; int pi=-1; boolean isNew;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_edit_product);
        db=AppData.get(this);
        pi=getIntent().getIntExtra("productIndex",-1);
        isNew=pi<0;
        Models.Product p=isNew?new Models.Product():db.products.get(pi);

        ((TextView)findViewById(R.id.tv_title)).setText(isNew?"Add Product":"Edit Product");
        findViewById(R.id.btn_back).setOnClickListener(v->finish());

        EditText etName=findViewById(R.id.et_name); etName.setText(p.name);
        EditText etBrand=findViewById(R.id.et_brand); etBrand.setText(p.brand);
        EditText etIngredients=findViewById(R.id.et_ingredients); etIngredients.setText(p.ingredients);
        EditText etConcerns=findViewById(R.id.et_concerns); etConcerns.setText(p.concerns);
        EditText etNotes=findViewById(R.id.et_notes); etNotes.setText(p.notes);

        Spinner typeSpinner=findViewById(R.id.spinner_type);
        String[] types={"face","body","both"};
        String[] typeLabels={"Face","Body","Face + Body"};
        ArrayAdapter<String> ta=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,typeLabels);
        ta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); typeSpinner.setAdapter(ta);
        for(int i=0;i<types.length;i++) if(types[i].equals(p.type)){typeSpinner.setSelection(i);break;}

        findViewById(R.id.btn_save).setOnClickListener(v->{
            String name=etName.getText().toString().trim();
            if(name.isEmpty()){etName.setError("Required");return;}
            Models.Product np=isNew?new Models.Product():db.products.get(pi);
            if(isNew) np.id="p"+System.currentTimeMillis();
            np.name=name;
            np.brand=etBrand.getText().toString().trim();
            np.type=types[typeSpinner.getSelectedItemPosition()];
            np.ingredients=etIngredients.getText().toString().trim();
            np.concerns=etConcerns.getText().toString().trim();
            np.notes=etNotes.getText().toString().trim();
            if(isNew) db.products.add(np); else db.products.set(pi,np);
            db.save(); finish();
        });
    }
}
