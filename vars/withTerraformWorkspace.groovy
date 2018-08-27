#!/usr/bin/env groovy

def call(ws, Closure body) {
      terraform.set_workspace(ws)
        body()
}

return this;
