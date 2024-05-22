package com.example.censoeducativo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterList extends RecyclerView.Adapter <AdapterList.ViewHolderData> {

   private List<DataList> dataList;

    public AdapterList(List<DataList> dataList){
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ViewHolderData onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listView = LayoutInflater.from(parent.getContext()).inflate(R.layout.send_list,parent,false);
        return new ViewHolderData(listView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderData holder, int position) {

        DataList listItem = dataList.get(position);

        holder.txtFecha.setText(listItem.getFecha());
        holder.txtCODMOD.setText(listItem.getCODMOD());
        holder.txtCENEDU.setText(listItem.getCENEDU());

        int intPrecisionStatus = 0;
        String strPrecision = listItem.getPrec();
        int intPrecisionValue =Double.valueOf(strPrecision).intValue(); // Try round to fix the problem

        if (intPrecisionValue > 0){
            if (intPrecisionValue < 12){
                intPrecisionStatus = 1;
            }
            else {
                if (intPrecisionValue < 51){
                    intPrecisionStatus = 2;
                }
                else {
                    intPrecisionStatus = 3;
                }
            }
        }
        holder.imgPrecision.setImageLevel(intPrecisionStatus);

        if (listItem.getSend().equals("1")) {
            holder.imgSend.setImageResource(R.drawable.ic_baseline_check);
        }
        else{
            if (listItem.getSend().equals("3")) {
                holder.imgSend.setImageResource(R.drawable.ic_baseline_cancel);
                holder.txtCENEDU.setText(R.string.invalid_code);
                holder.txtCENEDU.setTextColor(Color.parseColor("#D32F2F"));
            }
            else{
                if (listItem.getCENEDU().equals("")) {
                    holder.txtCENEDU.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        TextView txtFecha, txtCODMOD, txtCENEDU;
        ImageView imgSend, imgPrecision;

        public ViewHolderData(@NonNull View itemView) {
            super(itemView);

            txtFecha = itemView.findViewById(R.id.row_txtFecha);
            txtCODMOD = itemView.findViewById(R.id.row_txtCODMOD);
            txtCENEDU = itemView.findViewById(R.id.row_txtCENEDU);
            imgSend = itemView.findViewById(R.id.row_imgSend);
            imgPrecision = itemView.findViewById(R.id.row_imgPrecision);
        }


    }

}
