package ca.judacribz.week7day2_mvvm.viewmodel;

import android.text.Editable;
import android.util.Log;
import android.view.View;

import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.judacribz.week7day2_mvvm.R;
import ca.judacribz.week7day2_mvvm.model.datasource.remote.retrofit.UrbanDictionaryHelper;
import ca.judacribz.week7day2_mvvm.model.datasource.remote.retrofit.UrbanDictionaryObserver;
import ca.judacribz.week7day2_mvvm.model.urbandictionary.Definition;
import ca.judacribz.week7day2_mvvm.model.urbandictionary.Word;
import ca.judacribz.week7day2_mvvm.view.adapters.WordAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UrbanDictionaryViewModel extends ViewModel implements Observable, UrbanDictionaryObserver.DefinitionsListener {

    private PropertyChangeRegistry propertyChangeRegistry;
    private MutableLiveData<String> word;
    private MutableLiveData<List<Definition>> definitionsLiveData;

    @Bindable
    public WordAdapter wordAdapter;

    private UrbanDictionaryHelper urbanDictionaryHelper;

    public UrbanDictionaryViewModel() {
        propertyChangeRegistry = new PropertyChangeRegistry();
        word = new MutableLiveData<>();
        definitionsLiveData = new MutableLiveData<>();
        urbanDictionaryHelper = new UrbanDictionaryHelper();
    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        propertyChangeRegistry.add(callback);
    }

    public void afterWordChanged(Editable editable) {
        word.postValue(editable.toString());
    }

    public void onGetResults(View view) {
        String wordStr = word.getValue();

        if (wordStr != null) {
            urbanDictionaryHelper.getObsService().getDefinitions(wordStr)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new UrbanDictionaryObserver(this));
        }
    }

    @Override
    public void onDefinitionsReceived(@Nullable Word word) {
        if (word != null) {
            List<Definition> definitions = word.getDefinitions();
            if (definitions != null) {
                for (int i = 0; i < definitions.size(); i++) {
                    definitions.get(i).setRank(i + 1);
                }
                definitionsLiveData.setValue(definitions);
            }

        }
    }

    public void onSort(View view) {
        final List<Definition> definitions = definitionsLiveData.getValue();

        if (definitions != null) {
            DefinitionThumbsComparator definitionThumbsComparator = null;
            switch (view.getId()) {
                case R.id.ibtnThumbsUp:
                    definitionThumbsComparator = new DefinitionThumbsComparator(true);
                    break;
                case R.id.ibtnThumbsDown:
                    definitionThumbsComparator = new DefinitionThumbsComparator(false);
                    break;
            }

            if (definitionThumbsComparator != null) {
                Collections.sort(definitions, definitionThumbsComparator);
                definitionsLiveData.setValue(definitions);
            }
        }
    }

    public MutableLiveData<List<Definition>> getDefinitionsLiveData() {
        return definitionsLiveData;
    }

    public void setAdapter(@Nullable List<Definition> definitions) {
        wordAdapter = new WordAdapter(definitions);
        notifyAllPropertiesChanged();
    }

    private void notifyAllPropertiesChanged() {
        propertyChangeRegistry.notifyChange(this, 0);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        propertyChangeRegistry.remove(callback);
    }


    class DefinitionThumbsComparator implements Comparator<Definition> {
        private boolean sortByThumbsUp;

         DefinitionThumbsComparator(boolean sortByThumbsUp) {
            this.sortByThumbsUp = sortByThumbsUp;
        }

        @Override
        public int compare(Definition d1, Definition d2) {
            return Integer.compare(getThumbs(d2), getThumbs(d1));
        }

        private int getThumbs(Definition def) {
             return sortByThumbsUp ? def.getThumbsUp() : def.getThumbsDown();
        }
    }
}
