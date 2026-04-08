package com.skincare.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.skincare.app.R;
import com.skincare.app.data.*;

public class ProductsFragment extends Fragment {
    String[] CATS={"All","Cleanser","Toner","Serum","Moisturiser","SPF","Eye Cream","Mask","Treatment","Other"};
    int curCat=0;

    @Override public View onCreateView(LayoutInflater inf,ViewGroup c,Bundle b){
        View v=inf.inflate(R.layout.fragment_products,c,false);
        buildCats(v); buildList(v,AppData.get(requireContext()));
        v.findViewById(R.id.btn_new_product).setOnClickListener(x->startActivity(new Intent(getActivity(),EditProductActivity.class)));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildList(v,AppData.get(requireContext()));}

    void buildCats(View v){
        LinearLayout row=v.findViewById(R.id.cats_row);
        for(int i=0;i<CATS.length;i++){final int idx=i;
            TextView chip=new TextView(getContext()); chip.setText(CATS[i]);
            chip.setPadding(24,10,24,10); chip.setTextColor(0xFFFDF4FF); chip.setTextSize(12);
            chip.setBackgroundResource(idx==curCat?R.drawable.chip_active:R.drawable.chip_bg);
            chip.setOnClickListener(x->{curCat=idx;buildCats(v);buildList(v,AppData.get(requireContext()));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,8,0); chip.setLayoutParams(lp); row.addView(chip);}
    }

    void buildList(View v,AppData db){
        LinearLayout list=v.findViewById(R.id.products_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        for(Models.Product p:db.products){
            if(curCat>0&&!p.category.equals(CATS[curCat])) continue;
            View item=inf.inflate(R.layout.item_product,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(p.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(p.name);             ((TextView)item.findViewById(R.id.tv_brand)).setText(p.brand.isEmpty()?"No brand":p.brand);
            ((TextView)item.findViewById(R.id.tv_category)).setText(p.category);             String stars=""; for(int i=0;i<5;i++) stars+=(i<p.rating?"★":"☆");
            ((TextView)item.findViewById(R.id.tv_rating)).setText(stars);             item.setOnClickListener(x->{Intent i=new Intent(getActivity(),EditProductActivity.class);i.putExtra("product",p);startActivity(i);});
            list.addView(item);
        }
        if(list.getChildCount()==0){
            TextView empty=new TextView(getContext());             empty.setText("No products yet. Tap + to add your first product.");
            empty.setTextColor(0xFF6B5F7A); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32,60,32,60); list.addView(empty);
        }
    }
}
