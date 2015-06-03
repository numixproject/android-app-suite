package org.numixproject.hermes;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import org.numixproject.hermes.slides.FirstSlide;
import org.numixproject.hermes.slides.FourthSlide;
import org.numixproject.hermes.slides.SecondSlide;
import org.numixproject.hermes.slides.ThirdSlide;
import com.github.paolorotolo.appintro.AppIntro;

public class IntroActivity extends AppIntro {

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

    public void getStarted(View v){
        loadHermes();
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
