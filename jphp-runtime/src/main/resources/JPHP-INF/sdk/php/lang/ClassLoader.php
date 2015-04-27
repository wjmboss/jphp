<?php
namespace php\lang;

/**
 * Class ClassLoader
 * @package php\lang
 */
abstract class ClassLoader
{
    /**
     * @param string $name
     */
    abstract public function loadClass($name);

    public function register()
    {
    }

    public function unregister()
    {
    }
}