# Wonder 7.5-SNAPSHOT with wopatched 2.0.0

This branch builds Project Wonder `7.5-SNAPSHOT` against [wopatched](https://github.com/rparada/wopatching) `2.0.0`. 

`wopatched` artifacts replace replace the Apple WebObjects frameworks and already include the Wonder patches, including `ERFoundation` and `ERWebObjects`.

The general Project Wonder overview is in [README.mkd](README.mkd).

## Build

`wopatches7` has to be installed before `wopatched`. Wonder then resolves `wopatched` `2.0.0` from the local repository.

```bash
mkdir -p ~/scm
cd ~/scm
git clone https://github.com/rparada/wopatching.git
git clone https://github.com/rparada/wonder.git

cd ~/scm/wopatching/wopatches7
mvn clean install

cd ~/scm/wopatching/wopatched
mvn clean install

cd ~/scm/wonder
git checkout feature/wonder-7.5-snapshot-wopatched
mvn clean install
```

## Hello Wonder World

Two small applications exercise the result:

- `wopatched-examples/fluffy` uses the Fluffy Bunny layout (`Sources`, `Resources`, `Components`, `WebServerResources`). 
- `wopatched-examples/play` uses the Maven layout (`src/main`).

```bash
cd ~/scm
git clone https://github.com/rparada/wopatched-examples.git
```
