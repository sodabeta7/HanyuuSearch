package com.example.helloworld8;

/**
 * Created by sodabeta on 14-11-27.
 */
import java.io.File;

abstract class AlbumStorageDirFactory {
    public abstract File getAlbumStorageDir(String albumName);
}
