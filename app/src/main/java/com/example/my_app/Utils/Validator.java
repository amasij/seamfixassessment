package com.example.my_app.Utils;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;

import org.apache.commons.validator.routines.EmailValidator;


public class Validator {
    public static boolean isValidPassword(String password, Context context)
    {

        if(TextUtils.isEmpty(password)){
            return showMessage("Password cannot be blank",context);
        }
        if(password.length() < 6){
            return showMessage("Password must be at least 6 characters",context);
        }
        return true;
    }

    public static boolean isValidPhoneNumber(String password, Context context)
    {

        if(TextUtils.isEmpty(password)){
            return showMessage("Phone nnumber cannot be blank",context);
        }
        //assuming Nigerian numbers
        if(password.length() < 11){
            return showMessage("Not a valid phone number",context);
        }
        return true;
    }

    public static boolean isNotBlank(String field,String label, Context context){
        return !TextUtils.isEmpty(field) || showMessage(label +" cannot be blank", context);
    }

    public static boolean isNotNull(Object field,String label, Context context){
        return field != null || showMessage(label +" cannot be blank", context);
    }
    public static boolean isValidEmail(String email, Context context){
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email) || showMessage("Invalid Email",context);

    }

    public static boolean showMessage(String message,Context context){
        new StyleableToast
                .Builder(context)
                .text(message)
                .textColor(Color.WHITE)
                .backgroundColor(0xFFFC9999)
                .length(Toast.LENGTH_LONG)
                .show();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        return false;
    }



}
