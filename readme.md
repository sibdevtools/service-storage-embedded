# Simple Mock - Service Storage Local

## To build

```shell
chmod +x gradlew
./gradlew clean build
```

## Properties

| Key                                    | Description                                                                              | Default |
|----------------------------------------|------------------------------------------------------------------------------------------|---------|
| `service.local.storage.folder`         | Folder for data storing                                                                  | data    |
| `service.local.storage.buffer-size`    | Read content buffer max size                                                             | 1024    |
| `service.local.storage.storage-format` | Storage service data storing format. <br/>Support: <br/>* BASE64<br/>* BINARY<br/>* GZIP | GZIP    |

