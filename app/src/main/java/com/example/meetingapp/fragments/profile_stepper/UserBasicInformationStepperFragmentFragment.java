package com.example.meetingapp.fragments.profile_stepper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.CursorLoader;

import com.example.meetingapp.activities.CropImageActivity;
import com.example.meetingapp.utils.images.compression.Compressor;
import com.example.meetingapp.utils.images.DownloadImageTask;
import com.example.meetingapp.interfaces.GetImageFromAsync;
import com.example.meetingapp.interfaces.IUserProfileManager;
import com.example.meetingapp.R;
import com.example.meetingapp.api.RetrofitClient;
import com.example.meetingapp.models.ProfilePhoto;
import com.example.meetingapp.utils.PreferenceUtils;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class UserBasicInformationStepperFragmentFragment extends Fragment implements BlockingStep,
        DatePickerDialog.OnDateSetListener, GetImageFromAsync {

    public static final int IMAGE_REQUEST = 100;
    public static final int CAMERA_REQUEST = 101;
    private static final int CROP_REQUEST = 102;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @BindView(R.id.text_birth_date)
    MaterialEditText textBirthDate;
    @BindView(R.id.image_profile)
    CircleImageView imageProfile;
    @BindView(R.id.radio_group_sex)
    RadioGroup radioGroupSex;
    @BindView(R.id.layout_avatar_mask)
    RelativeLayout layoutAvatarMask;
    @BindView(R.id.header_h4)
    TextView headerH4;
    private IUserProfileManager iUserProfileManager;
    private FragmentActivity mContext;
    private String imageUrl;
    private Bitmap bitmap;
    private String pattern = "yyyy-MM-dd";
    private Date date;
    private String sex;

    public static UserBasicInformationStepperFragmentFragment newInstance() {
        return new UserBasicInformationStepperFragmentFragment();
    }

    private static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_info_stepper_fragment, container, false);
        ButterKnife.bind(this, view);

        int unicode = 0x1F607;
        headerH4.setText("Расскажите немного о себе " + new String(Character.toChars(unicode)));

        sex = "MALE";
        date = new Date();

        return view;
    }

    @OnClick({R.id.radio_male, R.id.radio_female, R.id.radio_do_not_know})
    void setUserGender(RadioButton radioButton) {
        boolean checked = radioButton.isChecked();

        switch (radioButton.getId()) {
            case R.id.radio_male:
                if (checked) sex = "MALE";
                break;
            case R.id.radio_female:
                if (checked) sex = "FEMALE";
                break;
            case R.id.radio_do_not_know:
                if (checked) sex = "UNSURE";
                break;
        }
    }

    @OnClick(R.id.text_birth_date)
    void setTextDate() {
        showDatePickerDialog();
    }

    @OnClick(R.id.image_profile)
    void openImage() {
        verifyStoragePermissions(requireActivity());
        selectImage();
    }

    private void selectImage() {
        final CharSequence[] items = { "Камера", "Галерея",
                "Отмена" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Фото профиля");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                Intent intent = new Intent(getContext(), CropImageActivity.class);
                if (items[item].equals("Камера")) {
                    intent.putExtra("request", CAMERA_REQUEST);
                    startActivityForResult(intent, CROP_REQUEST);
                } else if (items[item].equals("Галерея")) {
                    intent.putExtra("request", IMAGE_REQUEST);
                    startActivityForResult(intent, CROP_REQUEST);
                } else if (items[item].equals("Отмена")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private String getRealPathFromUri(Uri contentUri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(requireActivity(), contentUri, filePathColumn, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = Objects.requireNonNull(cursor).getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CROP_REQUEST && resultCode == RESULT_OK && data != null) {
            Bundle bundle = data.getExtras();
            Uri imageUri = data.getData();
            layoutAvatarMask.setVisibility(View.GONE);
            imageProfile.setImageURI(imageUri);
        }
    }

    @SuppressLint("WrongThread")
    @Override
    public void onNextClicked(StepperLayout.OnNextClickedCallback callback) {
        String date = Objects.requireNonNull(textBirthDate.getText()).toString();
        iUserProfileManager.saveBirthDate(date);
        iUserProfileManager.saveSex(sex);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        iUserProfileManager.savePhoto(byteArray);

        callback.goToNextStep();
    }

    @Override
    public void onCompleteClicked(StepperLayout.OnCompleteClickedCallback callback) {

    }

    @Override
    public void onBackClicked(StepperLayout.OnBackClickedCallback callback) {
        callback.goToPrevStep();
    }

    @Nullable
    @Override
    public VerificationError verifyStep() {
        if (Objects.requireNonNull(textBirthDate.getText()).toString().equals("")) {
            return new VerificationError("Пожалуйста, заполните все данные!");
        }
        if (bitmap == null){
            return new VerificationError("Добавьте фотографию, чтобы продолжить");
        }

        return null;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onError(@NonNull VerificationError error) {

    }

    private void showDatePickerDialog() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireActivity(),
                R.style.DialogTheme,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker();
        datePickerDialog.show();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = (FragmentActivity) context;

        if (context instanceof IUserProfileManager) {
            iUserProfileManager = (IUserProfileManager) context;
        } else {
            throw new IllegalStateException("Activity must implement IUserProfileManager interface!");
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        String strMonth = String.valueOf(month + 1);
        if (month + 1 < 10) {
            strMonth = "0" + (month + 1);
        }

        String strDay = String.valueOf(day);
        if (day < 10) {
            strDay = "0" + day;
        }

        String dateString = year + "-" + strMonth + "-" + strDay;
        textBirthDate.setText(dateString);
        setDateFromString(dateString);
    }

    private void setDateFromString(String dateString) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void upload(Uri imageUri) {
        String fullPath = getRealPathFromUri(imageUri);
        File file = new File(fullPath);

        File  compressFile = Compressor.getDefault(getContext()).compressToFile(file);

        RequestBody requestFile = RequestBody.create(compressFile, MediaType.parse(
                Objects.requireNonNull(requireActivity().getContentResolver().getType(imageUri))));

        Map<String, RequestBody> map = new HashMap<>();
        map.put("photo\"; filename=\"" + fullPath + "\"", requestFile);


        RetrofitClient.needsHeader(true);
        RetrofitClient.setToken(PreferenceUtils.getToken(requireContext()));
        Call<ProfilePhoto> call = RetrofitClient
                .getInstance(PreferenceUtils.getToken(requireContext()))
                .getApi()
                .uploadFile(map);

        call.enqueue(new Callback<ProfilePhoto>() {
            @Override
            public void onResponse(@NonNull Call<ProfilePhoto> call, @NonNull Response<ProfilePhoto> response) {
                ProfilePhoto profilePhoto = response.body();
                if (profilePhoto != null) {
                    Toast.makeText(getContext(), "Loaded!", Toast.LENGTH_SHORT).show();

                    imageUrl = profilePhoto.getPhoto();
                    new DownloadImageTask(UserBasicInformationStepperFragmentFragment.this).execute(imageUrl);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfilePhoto> call, @NonNull Throwable t) {
                Log.d("error", Objects.requireNonNull(t.getMessage()));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bitmap != null)
            imageProfile.setImageBitmap(bitmap);
    }

    @Override
    public void getResult(Bitmap bitmap) {
        layoutAvatarMask.setVisibility(View.GONE);
        imageProfile.setImageBitmap(bitmap);
        this.bitmap = bitmap;
    }
}
