# File-based configuration for local development

This is mostly intended for local development, you can create a files
under `~/.1config/` (in your home) and put the configuration for one
or more services in one or more environments with the following
format: `~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`

For example, these are all valid entries:

  - `~/.1config/service1/dev/3.2.0/service1.edn`
  - `~/.1config/service1/dev/3.10.6/service1.txt`
  - `~/.1config/user-database/dev/1.0.0/user-database.properties`
  - `~/.1config/service1/staging/3.10.6/service1.json`

The intended use of the configuration in `~/.1config/` is to facilitate
development and allow the service to start with a local configuration
which doesn't reside in your code.
