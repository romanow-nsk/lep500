package me.romanow.lep500;

import java.util.ArrayList;

public class FileDescriptionList extends ArrayList<FileDescription> {
    public void sort(I_FDComparator comparator){
        int sz = size();
        for(int i=0;i<sz;i++)
            for(int j=1;j<sz;j++)
                if (comparator.compare(get(j-1),get(j))<0){
                    FileDescription cc = get(j-1);
                    set(j-1,get(j));
                    set(j,cc);
                    }
    }
}
