package edu.amrita.jayadev.amritacafe.model

import edu.amrita.jayadev.amritacafe.menu.MenuItem

interface EditMenuItemListener {
    fun save(menuItem: MenuItem, position: Int?)
}