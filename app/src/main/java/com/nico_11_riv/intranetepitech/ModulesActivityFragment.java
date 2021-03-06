package com.nico_11_riv.intranetepitech;

import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nico_11_riv.intranetepitech.api.APIErrorHandler;
import com.nico_11_riv.intranetepitech.api.IntrAPI;
import com.nico_11_riv.intranetepitech.database.Allmodules;
import com.nico_11_riv.intranetepitech.database.Userinfos;
import com.nico_11_riv.intranetepitech.database.setters.modules.PAllModules;
import com.nico_11_riv.intranetepitech.database.setters.modules.PModules;
import com.nico_11_riv.intranetepitech.database.setters.user.GUser;
import com.nico_11_riv.intranetepitech.database.setters.user.GUserInfos;
import com.nico_11_riv.intranetepitech.database.setters.user.PUserInfos;
import com.nico_11_riv.intranetepitech.toolbox.CircleTransform;
import com.nico_11_riv.intranetepitech.toolbox.IsConnected;
import com.nico_11_riv.intranetepitech.ui.adapters.RVModulesAdapter;
import com.nico_11_riv.intranetepitech.ui.contents.ModuleContent;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.RestService;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by nicol on 15/03/2016.
 *
 */

@EFragment(R.layout.fragment_modules)
public class ModulesActivityFragment extends Fragment {

    private static int def_nb = 8;
    private static int def_semester = 11;
    private IsConnected is;

    @RestService
    IntrAPI api;

    @Bean
    APIErrorHandler ErrorHandler;

    @ViewById
    RecyclerView rv;

    @ViewById
    ProgressBar modules_progress;

    @ViewById
    TextView noinfos;

    private GUser user = new GUser();
    private GUserInfos user_info = new GUserInfos();
    private RVModulesAdapter adapter;

    @UiThread
    public void mToast(String msg, int time) {
        Toast.makeText(getContext(), msg, time).show();
    }

    @AfterInject
    void afterInject() {
        api.setRestErrorHandler(ErrorHandler);
    }

    @UiThread
    void setadpt(ArrayList<ModuleContent> items) {
        adapter = new RVModulesAdapter(items, getContext());
        rv.setAdapter(adapter);
    }

    @UiThread
    void filluserinfosui() {
        TextView tv = (TextView) getActivity().findViewById(R.id.menu_login);
        tv.setText(user_info.getLogin());
        tv = (TextView) getActivity().findViewById(R.id.menu_email);
        tv.setText(user_info.getEmail());

        ImageView iv = (ImageView) getActivity().findViewById(R.id.menu_img);
        Picasso.with(getContext()).load(user_info.getPicture()).transform(new CircleTransform()).into(iv);
    }

    void fillmodulesui() {
        ArrayList<ModuleContent> items = new ArrayList<>();
        List<Allmodules> modules = Select.from(Allmodules.class).where(Condition.prop("login").eq(user.getLogin())).list();

        for (int i = 0; i < modules.size(); i++) {
            Allmodules info = modules.get(i);
            items.add(new ModuleContent(info.getGrade(), info.getTitle(), info.getBegin() + " -> " + info.getEnd(), info.getCode()));
        }
        setadpt(items);
    }

    @UiThread
    void fillnewmodulesui() {
        if (def_nb != 8) {
            if (def_semester != 11)
                adapter.filter((def_nb + 1) * 5, "B" + Integer.toString(def_semester) + "%");
            else
                adapter.filter((def_nb + 1) * 5, "All");
        } else {
            if (def_semester != 11)
                adapter.filter(0, "B" + Integer.toString(def_semester) + "%");
            else
                adapter.filter(0, "All");
        }
    }

    void setUserInfos() {
        List<Userinfos> uInfos = Userinfos.findWithQuery(Userinfos.class, "SELECT * FROM Userinfos WHERE login = ?", user.getLogin());
        if (uInfos.size() > 0)
            filluserinfosui();
        if (is.connected()) {
            Userinfos.deleteAll(Userinfos.class, "login = ?", user.getLogin());
            api.setCookie("PHPSESSID", user.getToken());
            try {
                PUserInfos infos = new PUserInfos();
                infos.init(api.getuserinfo(user.getLogin()));
            } catch (HttpClientErrorException e) {
                Log.d("Response", e.getResponseBodyAsString());
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
            } catch (NullPointerException e) {
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
            user_info = new GUserInfos();
            filluserinfosui();
        }
    }

    void setModules() {
        fillmodulesui();
        if (is.connected()) {
            String m = null;
            api.setCookie("PHPSESSID", user.getToken());
            try {
                m = api.getmarksandmodules(user.getLogin());
            } catch (HttpClientErrorException e) {
                Log.d("Response", e.getResponseBodyAsString());
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
            } catch (NullPointerException e) {
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
            PModules modules = new PModules();
            modules.init(m);
            api.setCookie("PHPSESSID", user.getToken());
            try {
                api.getuserinfo(user.getLogin());
            } catch (HttpClientErrorException e) {
                Log.d("Response", e.getResponseBodyAsString());
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
            } catch (NullPointerException e) {
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
            api.setCookie("PHPSESSID", user.getToken());
            try {
                PAllModules mod = new PAllModules();
                mod.init(api.getallmodules());
            } catch (HttpClientErrorException e) {
                Log.d("Response", e.getResponseBodyAsString());
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
            }  catch (NullPointerException e) {
                mToast(e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
            fillnewmodulesui();
        }
    }

    @UiThread
    void setProgressBar() {
        modules_progress.setVisibility(View.GONE);
        List<Allmodules> m = Allmodules.findWithQuery(Allmodules.class, "SELECT * FROM Allmodules WHERE login = ?", user.getLogin());
        if (m.size() < 1)
            noinfos.setVisibility(View.VISIBLE);
    }

    @Background
    void profile_modules() {
        setUserInfos();
        setModules();
        setProgressBar();
    }

    @AfterViews
    void init() {
        is = new IsConnected(getActivity().getApplicationContext());
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        profile_modules();
    }

    public void filter(int size, String semester, int def_nb_act, int def_semester_act) {
        if (adapter != null) {
            def_nb = def_nb_act;
            def_semester = def_semester_act;
            adapter.filter(size, semester);
        } else
            mToast("Attendez le chargement de la page", Toast.LENGTH_SHORT);
    }

    public void search(String text) {
        if (adapter != null)
            adapter.search(text);
        else
            mToast("Attendez le chargement de la page", Toast.LENGTH_SHORT);
    }
}
