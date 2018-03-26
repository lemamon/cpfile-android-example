package com.example.dem.cpfiles;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    private Uri source;
    private Uri target;

    private final int REQUEST_CODE_SOURCE = 42;
    private final int REQUEST_CODE_TARGET = 43;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                }, 44);

        ctx = this;

        Button btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean okCopy = copy(
                    new File(FileUtil.getPath(ctx, source)),
                    FileUtil.getFullPathFromTreeUri(target, ctx),
                    ctx
                );
                setText(R.id.txt_copy, okCopy ? "Copy Ok" : "Copy NotOk");
            }
        });

        Button btnTarget = findViewById(R.id.btn_target);
        btnTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE_TARGET);
            }
        });

        Button btnSource = findViewById(R.id.btn_source);
        btnSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent openIntent = new Intent(Intent.ACTION_GET_CONTENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                openIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openIntent.setType("*/*");
                startActivityForResult(openIntent, REQUEST_CODE_SOURCE);
            }
        });
    }


    public  void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(data == null) return;

        switch (requestCode){
            case REQUEST_CODE_SOURCE:
                source = data.getData();
                takeFlags(data, source);
                setText(R.id.txt_source, FileUtil.getPath(ctx, source));
                break;
            case REQUEST_CODE_TARGET:
                target = data.getData();
                takeFlags(data, target);
                setText(R.id.txt_target, FileUtil.getFullPathFromTreeUri(target, ctx));
                break;
            default:
                System.out.println("No Option");
                break;
        }
    }

    private  void takeFlags(Intent data, Uri dir) {
        final int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(dir, takeFlags);
    }

    private void setText(int id, String txt ) {
        TextView textView = findViewById(id);
        textView.setText(txt);
    }

    public static boolean copy(File copy, String directory, Context con) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        DocumentFile dir= getDocumentFileIfAllowedToWrite(new File(directory), con);
        String mime = mime(copy.toURI().toString());
        DocumentFile copy1= dir.createFile(mime, copy.getName());
        try {
            inStream = new FileInputStream(copy);
            outStream =
                    con.getContentResolver().openOutputStream(copy1.getUri());
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
                outStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static DocumentFile getDocumentFileIfAllowedToWrite(File file, Context con) {

        List<UriPermission> permissionUris = con.getContentResolver().getPersistedUriPermissions();

        for (UriPermission permissionUri : permissionUris) {
            Log.i("URI Permission", permissionUri.getUri().toString());
            Uri treeUri = permissionUri.getUri();
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(con, treeUri);
            String rootDocFilePath = FileUtil.getFullPathFromTreeUri(treeUri, con);

            if (file.getAbsolutePath().startsWith(rootDocFilePath)) {

                ArrayList<String> pathInRootDocParts = new ArrayList<String>();
                while (!rootDocFilePath.equals(file.getAbsolutePath())) {
                    pathInRootDocParts.add(file.getName());
                    file = file.getParentFile();
                }

                DocumentFile docFile = null;

                if (pathInRootDocParts.size() == 0) {
                    docFile = DocumentFile.fromTreeUri(con, rootDocFile.getUri());
                } else {
                    for (int i = pathInRootDocParts.size() - 1; i >= 0; i--) {
                        if (docFile == null) {
                            docFile = rootDocFile.findFile(pathInRootDocParts.get(i));
                        } else {
                            docFile = docFile.findFile(pathInRootDocParts.get(i));
                        }
                    }
                }
                if (docFile != null && docFile.canWrite()) {
                    return docFile;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public static String mime(String URI) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(URI);
        return ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) : "";
    }

}
