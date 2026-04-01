let
  nixpkgs = fetchTarball "https://github.com/NixOS/nixpkgs/tarball/107cba9eb4a8d8c9f8e9e61266d78d340867913a";
  pkgs = import nixpkgs {
    config = { };
    overlays = [ ];
  };
  # z3 package in nix isn't built with java bindings enabled by default, so override is needed
  z3 = pkgs.z3.override {
    javaBindings = true;
    inherit jdk;
  };
  jdk = pkgs.jdk21;
in

pkgs.mkShell {
  packages =
    [
      jdk
      z3
      pkgs.maven
    ];

    shellHook = ''
      # set env variables so z3 can find z3 binary and lib
      export LD_LIBRARY_PATH=${z3.java}/lib:$LD_LIBRARY_PATH
      export DYLD_LIBRARY_PATH=${z3}/bin
      mvn install:install-file -Dfile="${z3.java}/share/java/com.microsoft.z3.jar" -DgroupId=com.microsoft -DartifactId=z3 -Dversion=4.13.3 -Dpackaging=jar -DgeneratePom=true
    '';
}
