package org.numixproject.hermes;
import android.content.Intent;
import android.os.Bundle;

import org.numixproject.hermes.slides.FirstSlide;
import org.numixproject.hermes.slides.FourthSlide;
import org.numixproject.hermes.slides.SecondSlide;
import org.numixproject.hermes.slides.ThirdSlide;
import org.numixproject.hermes.utils.AndroidIntro;

public class IntroActivity extends AndroidIntro {

    @Override
    public void init(Bundle savedInstanceState) {
        addSlide(new FirstSlide(), getApplicationContext());
        addSlide(new SecondSlide(), getApplicationContext());
        addSlide(new ThirdSlide(), getApplicationContext());
        addSlide(new FourthSlide(), getApplicationContext());
    }

    private void loadHermes(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSkipPressed() {
        loadHermes();
    }

    @Override
    public void onDonePressed() {
        loadHermes();
    }
}
