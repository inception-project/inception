/**
 * BootSideMenu v 2.0
 * Author: Andrea Lombardo
 * http://www.lombardoandrea.com
 * https://github.com/AndreaLombardo/BootSideMenu
 * */
(function ($) {

    $.fn.BootSideMenu = function (userOptions) {

        var initialCode;
        var newCode;
        var $menu;
        var prevStatus;
        var bodyProperties = {};

        var hoverStatus;

        var $DOMBody = $("");

        var defaults = {
            side: "left",
            duration: 500,
            remember: true,
            autoClose: false,
            pushBody: true,
            closeOnClick: true,
            icons: {
                left: 'glyphicon glyphicon-chevron-left',
                right: 'glyphicon glyphicon-chevron-right',
                down: 'glyphicon glyphicon-chevron-down'
            },
            theme: 'default',
            width: "50%",
            onTogglerClick: function () {
                //code to be executed when the toggler arrow was clicked
            },
            onBeforeOpen: function () {
                //code to be executed before menu open
            },
            onBeforeClose: function () {
                //code to be executed before menu close
            },
            onOpen: function () {
                //code to be executed after menu open
            },
            onClose: function () {
                //code to be executed after menu close
            },
            onStartup: function () {
                //code to be executed when the plugin is called
            }
        };

        var options = $.extend({}, defaults, userOptions);


        bodyProperties['originalMarginLeft'] = $DOMBody.css("margin-left");
        bodyProperties['originalMarginRight'] = $DOMBody.css("margin-right");
        bodyProperties['width'] = $DOMBody.width();

        initialCode = this.html();

        newCode = '<div class="menu-wrapper">' + initialCode + '</div>';
        newCode += '<div class="toggler" data-whois="toggler">';
        newCode += '<span class="icon">&nbsp;</span>';
        newCode += '</div>';

        this.empty();
        this.html(newCode);

        $menu = $(this);

        $menu.addClass("container");
        $menu.addClass("bootsidemenu");
        $menu.addClass(options.theme);
        $menu.css("width", options.width);

        if (options.side === "left") {
            $menu.addClass("bootsidemenu-left");
        } else if (options.side === "right") {
            $menu.addClass("bootsidemenu-right");
        }

        $menu.id = $menu.attr("id");
        $menu.cookieName = "bsm2-" + $menu.id;
        $menu.toggler = $menu.find('[data-whois="toggler"]');
        $menu.originalPushBody = options.pushBody;
        $menu.originalCloseOnClick = options.closeOnClick;


        if (options.remember) {
            prevStatus = readCookie($menu.cookieName);
        } else {
            prevStatus = null;
        }


        forSmallBody();

        switch (prevStatus) {
            case "opened":
                startOpened();
                break;
            case "closed":
                startClosed();
                break;
            default:
                startDefault();
                break;
        }

        if (options.onStartup !== undefined && isFunction(options.onStartup)) {
            options.onStartup($menu);
        }

        $('[data-toggle="collapse"]', $menu).each(function () {
            var $icon = $('<span/>');
            $icon.addClass('icon');
            $icon.addClass(options.icons.right);

            $(this).prepend($icon);
        });

        $menu.off('click', '.toggler[data-whois="toggler"]', toggle);
        $menu.on('click', '.toggler[data-whois="toggler"]', toggle);

        $menu.off('click', '.list-group-item');
        $menu.on('click', '.list-group-item', function () {
            $menu.find(".list-group-item").each(function () {
                $(this).removeClass("active");
            });
            $(this).addClass('active');
            $('.icon', $(this)).toggleClass(options.icons.right).toggleClass(options.icons.down);
        });

        $menu.off('click', 'a.list-group-item', onItemClick);
        $menu.on('click', 'a.list-group-item', onItemClick);

        $menu.off('mouseenter mouseleave');
        $menu.hover(menuOnHoverIn, menuOnHoverOut);

        $(document).on('click', function () {
            if (options.closeOnClick && (!hoverStatus)) {
                closeMenu(true);
            }
        });

        function menuOnHoverOut() {
            hoverStatus = false;
        }

        function menuOnHoverIn() {
            hoverStatus = true;
        }

        function onItemClick() {
            if (options.closeOnClick && ($(this).attr('data-toggle') !== 'collapse')) {
                closeMenu(true);
            }
        }

        function toggle() {

            if (options.onTogglerClick !== undefined && isFunction(options.onTogglerClick)) {
                options.onTogglerClick($menu);
            }

            if ($menu.status === "opened") {
                closeMenu(true);
            } else {
                openMenu(true);
            }
        }

        function switchArrow(side) {
            var $icon = $menu.toggler.find(".icon");

            $icon.removeClass();

            if (side === "left") {
                $icon.addClass(options.icons.right);
            } else if (side === "right") {
                $icon.addClass(options.icons.left);
            }

            $icon.addClass('icon');
        }

        function startDefault() {
            if (options.side === "left") {
                if (options.autoClose) {
                    $menu.status = "closed";
                    $menu.hide().animate({
                        left: -($menu.width() + 2)
                    }, 1, function () {
                        $menu.show();
                        switchArrow("left");
                    });
                } else if (!options.autoClose) {
                    switchArrow("right");
                    $menu.status = "opened";
                    if (options.pushBody) {
                        $DOMBody.css("margin-left", $menu.width() + 20);
                    }
                }
            } else if (options.side === "right") {
                if (options.autoClose) {
                    $menu.status = "closed";
                    $menu.hide().animate({
                        right: -($menu.width() + 2)
                    }, 1, function () {
                        $menu.show();
                        switchArrow("right");
                    });
                } else {
                    switchArrow("left");
                    $menu.status = "opened";
                    if (options.pushBody) {
                        $DOMBody.css("margin-right", $menu.width() + 20);
                    }
                }
            }
        }

        function startClosed() {
            if (options.side === "left") {
                $menu.status = "closed";
                $menu.hide().animate({
                    left: -($menu.width() + 2)
                }, 1, function () {
                    $menu.show();
                    switchArrow("left");
                });
            } else if (options.side === "right") {
                $menu.status = "closed";
                $menu.hide().animate({
                    right: -($menu.width() + 2)
                }, 1, function () {
                    $menu.show();
                    switchArrow("right");
                })
            }
        }

        function startOpened() {
            if (options.side === "left") {
                switchArrow("right");
                $menu.status = "opened";
                if (options.pushBody) {
                    $DOMBody.css("margin-left", $menu.width() + 20);
                }

            } else if (options.side === "right") {
                switchArrow("left");
                $menu.status = "opened";
                if (options.pushBody) {
                    $DOMBody.css("margin-right", $menu.width() + 20);
                }
            }
        }

        function closeMenu(execFunctions) {
            if (execFunctions) {
                if (options.onBeforeClose !== undefined && isFunction(options.onBeforeClose)) {
                    options.onBeforeClose($menu);
                }
            }
            if (options.side === "left") {

                if (options.pushBody) {
                    $DOMBody.animate({marginLeft: bodyProperties.originalMarginLeft}, {duration: options.duration});
                }

                $menu.animate({
                    left: -($menu.width() + 2)
                }, {
                    duration: options.duration,
                    done: function () {
                        switchArrow("left");
                        $menu.status = "closed";

                        if (execFunctions) {
                            if (options.onClose !== undefined && isFunction(options.onClose)) {
                                options.onClose($menu);
                            }
                        }
                    }
                });
            } else if (options.side === "right") {

                if (options.pushBody) {
                    $DOMBody.animate({marginRight: bodyProperties.originalMarginRight}, {duration: options.duration});
                }

                $menu.animate({
                    right: -($menu.width() + 2)
                }, {
                    duration: options.duration,
                    done: function () {
                        switchArrow("right");
                        $menu.status = "closed";

                        if (execFunctions) {
                            if (options.onClose !== undefined && isFunction(options.onClose)) {
                                options.onClose($menu);
                            }
                        }
                    }
                });
            }

            if (options.remember) {
                storeCookie($menu.cookieName, "closed");
            }

        }

        function openMenu(execFunctions) {

            if (execFunctions) {
                if (options.onBeforeOpen !== undefined && isFunction(options.onBeforeOpen)) {
                    options.onBeforeOpen($menu);
                }
            }

            if (options.side === "left") {

                if (options.pushBody) {
                    $DOMBody.animate({marginLeft: $menu.width() + 20}, {duration: options.duration});
                }

                $menu.animate({
                    left: 0
                }, {
                    duration: options.duration,
                    done: function () {
                        switchArrow("right");
                        $menu.status = "opened";

                        if (execFunctions) {
                            if (options.onOpen !== undefined && isFunction(options.onOpen)) {
                                options.onOpen($menu);
                            }
                        }
                    }
                });
            } else if (options.side === "right") {

                if (options.pushBody) {
                    $DOMBody.animate({marginRight: $menu.width() + 20}, {duration: options.duration});
                }

                $menu.animate({
                    right: 0
                }, {
                    duration: options.duration,
                    done: function () {
                        switchArrow("left");
                        $menu.status = "opened";

                        if (execFunctions) {
                            if (options.onOpen !== undefined && isFunction(options.onOpen)) {
                                options.onOpen($menu);
                            }
                        }
                    }
                });
            }

            if (options.remember) {
                storeCookie($menu.cookieName, "opened");
            }
        }


        function forSmallBody() {
            var windowWidth = $(window).width();

            if (windowWidth <= 480) {
                options.pushBody = false;
                options.closeOnClick = true;
            } else {
                options.pushBody = $menu.originalPushBody;
                options.closeOnClick = $menu.originalCloseOnClick;
            }
        }

        function storeCookie(nome, valore) {
            var d = new Date();
            d.setTime(d.getTime() + (24 * 60 * 60 * 1000));
            var expires = "expires=" + d.toUTCString();
            document.cookie = nome + "=" + valore + "; " + expires + "; path=/";
        }

        function readCookie(nome) {
            var name = nome + "=";
            var ca = document.cookie.split(";");
            for (var i = 0; i < ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0) === " ")
                    c = c.substring(1);
                if (c.indexOf(name) === 0) return c.substring(name.length, c.length);
            }
            return null;
        }

        function isFunction(functionToCheck) {
            var getType = {};
            return functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
        }

        function onResize() {
            forSmallBody();
            if ($menu.status === "closed") {
                startClosed();
            }
            if ($menu.status === "opened") {
                startOpened();
            }
        }

        var resizeStart;
        var resizeEnd;
        var wait = 250;
        window.addEventListener("resize", function () {
            resizeStart = new Date().getMilliseconds();
            resizeEnd = resizeStart + wait;
            setTimeout(function () {
                var now = new Date().getMilliseconds();
                if (now > resizeEnd) {
                    onResize();
                }
            }, wait);
        }, false);


        $.fn.BootSideMenu.open = function () {
            openMenu();
        };

        $.fn.BootSideMenu.close = function () {
            closeMenu();
        };

        $.fn.BootSideMenu.toggle = function () {
            toggle();
        };

        return this;

    }
}(jQuery));
