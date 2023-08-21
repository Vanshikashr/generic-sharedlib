package com.packages
// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}
